using System.Linq.Expressions;
using CsuPlusPlus.Dtos.Common;
using CsuPlusPlus.Dtos.Course;
using CsuPlusPlus.Infra;
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

    public async Task<GetCourseResponse> GetByUniversity(long universityId, PageRequest request,
        CancellationToken cancellationToken)
    {
        if (!await db.Universities.AnyAsync(u => u.Id == universityId, cancellationToken))
            return new GetCourseResponse(GetCourseResult.UniversityNotFound);

        var query = db.Courses.AsNoTracking().Where(c => c.University.Id == universityId);
        var totalCount = await query.LongCountAsync(cancellationToken);
        var items = await query
            .OrderBy(c => c.Id)
            .Skip(request.Skip)
            .Take(request.PageSize)
            .Select(ToResponse)
            .ToListAsync(cancellationToken);

        return new GetCourseResponse(GetCourseResult.Success,
            new PagedResponse<CourseResponse>(items, request.Page, request.PageSize, totalCount));
    }

    public async Task<GetCourseResponse> GetByCollege(long collegeId, PageRequest request,
        CancellationToken cancellationToken, long? universityId = null)
    {
        var collegeUniversityId = await db.Colleges.AsNoTracking()
            .Where(c => c.Id == collegeId)
            .Select(c => (long?)c.University.Id)
            .SingleOrDefaultAsync(cancellationToken);

        if (collegeUniversityId is null)
            return new GetCourseResponse(GetCourseResult.CollegeNotFound);

        if (universityId is { } scope && collegeUniversityId != scope)
            return new GetCourseResponse(GetCourseResult.CollegeInOtherUniversity);

        var query = db.Courses.AsNoTracking()
            .Where(c => c.College != null && c.College.Id == collegeId);
        var totalCount = await query.LongCountAsync(cancellationToken);
        var items = await query
            .OrderBy(c => c.Id)
            .Skip(request.Skip)
            .Take(request.PageSize)
            .Select(ToResponse)
            .ToListAsync(cancellationToken);

        return new GetCourseResponse(GetCourseResult.Success,
            new PagedResponse<CourseResponse>(items, request.Page, request.PageSize, totalCount));
    }

    public async Task<GetSingleCourseResponse> GetById(long id, CancellationToken cancellationToken,
        long? universityId = null)
    {
        var courseUniversityId = await db.Courses.AsNoTracking()
            .Where(c => c.Id == id)
            .Select(c => (long?)c.University.Id)
            .SingleOrDefaultAsync(cancellationToken);

        if (courseUniversityId is null)
            return new GetSingleCourseResponse(GetCourseResult.CourseNotFound);

        if (universityId is { } scope && courseUniversityId != scope)
            return new GetSingleCourseResponse(GetCourseResult.CourseInOtherUniversity);

        var course = await db.Courses.AsNoTracking()
            .Where(c => c.Id == id)
            .Select(ToResponse)
            .SingleAsync(cancellationToken);

        return new GetSingleCourseResponse(GetCourseResult.Success, course);
    }
}