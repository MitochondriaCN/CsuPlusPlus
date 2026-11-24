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

    public async Task<GetCourseResponse> GetByUniversityAsync(long universityId, PageRequest request,
        CancellationToken cancellationToken)
    {
        if (!await db.Universities.AnyAsync(u => u.Id == universityId, cancellationToken))
            return new GetCourseResponse(GetCourseStatus.UniversityNotFound);

        var query = db.Courses.AsNoTracking().Where(c => c.University.Id == universityId);
        var totalCount = await query.LongCountAsync(cancellationToken);
        var items = await query
            .OrderBy(c => c.Id)
            .Skip(request.Skip)
            .Take(request.PageSize)
            .Select(ToResponse)
            .ToListAsync(cancellationToken);

        return new GetCourseResponse(GetCourseStatus.Success,
            new PagedResponse<CourseResponse>(items, request.Page, request.PageSize, totalCount));
    }

    public async Task<GetCourseResponse> GetByCollegeAsync(long collegeId, PageRequest request,
        CancellationToken cancellationToken, long? universityId = null)
    {
        var collegeUniversityId = await db.Colleges.AsNoTracking()
            .Where(c => c.Id == collegeId)
            .Select(c => (long?)c.University.Id)
            .SingleOrDefaultAsync(cancellationToken);

        if (collegeUniversityId is null)
            return new GetCourseResponse(GetCourseStatus.CollegeNotFound);

        if (universityId is { } scope && collegeUniversityId != scope)
            return new GetCourseResponse(GetCourseStatus.CollegeInOtherUniversity);

        var query = db.Courses.AsNoTracking()
            .Where(c => c.College != null && c.College.Id == collegeId);
        var totalCount = await query.LongCountAsync(cancellationToken);
        var items = await query
            .OrderBy(c => c.Id)
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
}