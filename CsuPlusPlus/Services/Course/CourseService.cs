using System.Linq.Expressions;
using CsuPlusPlus.Dtos.Common;
using CsuPlusPlus.Dtos.Course;
using CsuPlusPlus.Infra;
using CsuPlusPlus.Models.ValueObjects;
using Microsoft.EntityFrameworkCore;

namespace CsuPlusPlus.Services.Course;

public class CourseService(AppDbContext db) : ICourseService
{
    private static readonly Expression<Func<Models.Course, CourseResponse>> ToResponse =
        c => new CourseResponse(
            c.Id,
            c.Name.Value,
            c.University.Id,
            c.University.Name.Value,
            c.College == null ? null : c.College.Id,
            c.College == null ? null : c.College.Name.Value,
            c.Alias == null ? null : c.Alias.Value,
            c.Hours,
            c.CreatedAt,
            c.UpdatedAt);

    public async Task<GetCourseResponse> ListAsync(
        PageRequest request,
        CancellationToken cancellationToken,
        long? universityId = null,
        long? collegeId = null,
        string? keyword = null)
    {
        var query = db.Courses.AsNoTracking();

        if (universityId is { } targetUniversityId)
        {
            if (!await db.Universities.AnyAsync(u => u.Id == targetUniversityId, cancellationToken))
                return new GetCourseResponse(GetCourseStatus.UniversityNotFound);

            query = query.Where(c => c.University.Id == targetUniversityId);
        }

        if (collegeId is { } targetCollegeId)
        {
            var collegeUniversityId = await db.Colleges.AsNoTracking()
                .Where(c => c.Id == targetCollegeId)
                .Select(c => (long?)c.University.Id)
                .SingleOrDefaultAsync(cancellationToken);

            if (collegeUniversityId is null)
                return new GetCourseResponse(GetCourseStatus.CollegeNotFound);

            if (universityId is { } scope && collegeUniversityId != scope)
                return new GetCourseResponse(GetCourseStatus.CollegeInOtherUniversity);

            query = query.Where(c => c.College != null && c.College.Id == targetCollegeId);
        }

        if (!string.IsNullOrWhiteSpace(keyword))
        {
            var term = keyword.Trim();
            if (db.Database.IsNpgsql())
            {
                var pattern = $"%{EscapeLikePattern(term)}%";
                query = query
                    .Where(c =>
                        EF.Functions.ILike(c.Name.Value, pattern, "\\")
                        || EF.Functions.TrigramsAreSimilar(c.Name.Value, term)
                        || (c.Alias != null && (
                            EF.Functions.ILike(c.Alias.Value, pattern, "\\")
                            || EF.Functions.TrigramsAreSimilar(c.Alias.Value, term))))
                    .OrderByDescending(c =>
                        EF.Functions.ILike(c.Name.Value, pattern, "\\")
                        || (c.Alias != null && EF.Functions.ILike(c.Alias.Value, pattern, "\\")))
                    .ThenByDescending(c => Math.Max(
                        EF.Functions.TrigramsSimilarity(c.Name.Value, term),
                        EF.Functions.TrigramsSimilarity(c.Alias == null ? "" : c.Alias.Value, term)))
                    .ThenBy(c => c.Id);
            }
            else
            {
                var lowerTerm = term.ToLowerInvariant();
                query = query
                    .Where(c =>
                        c.Name.Value.ToLower().Contains(lowerTerm)
                        || (c.Alias != null && c.Alias.Value.ToLower().Contains(lowerTerm)))
                    .OrderBy(c => c.Id);
            }
        }
        else
        {
            query = query.OrderBy(c => c.Id);
        }

        var totalCount = await query.LongCountAsync(cancellationToken);
        var items = await query
            .Skip(request.Skip)
            .Take(request.PageSize)
            .Select(ToResponse)
            .ToListAsync(cancellationToken);

        return new GetCourseResponse(GetCourseStatus.Success,
            new PagedResponse<CourseResponse>(items, request.Page, request.PageSize, totalCount));
    }

    public async Task<GetSingleCourseResponse> GetByIdAsync(long id, CancellationToken cancellationToken,
        long? universityId = null)
    {
        var courseUniversityId = await db.Courses.AsNoTracking()
            .Where(c => c.Id == id)
            .Select(c => (long?)c.University.Id)
            .SingleOrDefaultAsync(cancellationToken);

        if (courseUniversityId is null)
            return new GetSingleCourseResponse(GetCourseStatus.CourseNotFound);

        if (universityId is { } scope && courseUniversityId != scope)
            return new GetSingleCourseResponse(GetCourseStatus.CourseInOtherUniversity);

        var course = await db.Courses.AsNoTracking()
            .Where(c => c.Id == id)
            .Select(ToResponse)
            .SingleAsync(cancellationToken);

        return new GetSingleCourseResponse(GetCourseStatus.Success, course);
    }

    public async Task<CreateCourseResponse> CreateAsync(long universityId, CreateCourseRequest request,
        CancellationToken cancellationToken)
    {
        var university = await db.Universities
            .SingleOrDefaultAsync(u => u.Id == universityId, cancellationToken);
        if (university is null)
            return new CreateCourseResponse(CreateCourseStatus.UniversityNotFound);

        Models.College? college = null;
        if (request.CollegeId is { } collegeId)
        {
            college = await db.Colleges
                .Include(c => c.University)
                .SingleOrDefaultAsync(c => c.Id == collegeId, cancellationToken);
            if (college is null)
                return new CreateCourseResponse(CreateCourseStatus.CollegeNotFound);
            if (college.University.Id != universityId)
                return new CreateCourseResponse(CreateCourseStatus.CollegeInOtherUniversity);
        }

        var now = DateTimeOffset.UtcNow;
        var course = new Models.Course
        {
            Name = new NonEmptyName(request.Name),
            University = university,
            College = college,
            Alias = string.IsNullOrWhiteSpace(request.Alias) ? null : new NonEmptyName(request.Alias),
            Hours = request.Hours,
            CreatedAt = now,
            UpdatedAt = now
        };

        db.Courses.Add(course);
        await db.SaveChangesAsync(cancellationToken);

        var created = await GetByIdAsync(course.Id, cancellationToken);
        return new CreateCourseResponse(CreateCourseStatus.Success, created.Response);
    }

    public async Task<DeleteCourseResponse> DeleteAsync(long id, CancellationToken cancellationToken,
        long? universityId = null)
    {
        var course = await db.Courses
            .Include(c => c.University)
            .SingleOrDefaultAsync(c => c.Id == id, cancellationToken);
        if (course is null)
            return new DeleteCourseResponse(DeleteCourseStatus.CourseNotFound);

        if (universityId is { } scope && course.University.Id != scope)
            return new DeleteCourseResponse(DeleteCourseStatus.CourseInOtherUniversity);

        var courseFiles = await db.CourseFiles
            .Where(cf => cf.CourseId == id)
            .ToListAsync(cancellationToken);
        db.CourseFiles.RemoveRange(courseFiles);

        db.Courses.Remove(course);
        await db.SaveChangesAsync(cancellationToken);

        return new DeleteCourseResponse(DeleteCourseStatus.Success);
    }

    private static string EscapeLikePattern(string value) =>
        value
            .Replace("\\", "\\\\")
            .Replace("%", "\\%")
            .Replace("_", "\\_");
}