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

    public async Task<GetCourseResponse> GetByUniversity(long universityId, CancellationToken cancellationToken)
    {
        if (!await db.Universities.AnyAsync(u => u.Id == universityId, cancellationToken))
            return new GetCourseResponse(GetCourseResult.UniversityNotFound, PagedResponse<CourseResponse>.EmptyPage);

        var items = await db.Courses.AsNoTracking()
            .Where(c => c.University.Id == universityId)
            .OrderBy(c => c.Id)
            .Select(ToResponse)
            .ToListAsync(cancellationToken);

        return new GetCourseResponse(GetCourseResult.Success, ToPage(items));
    }

    public async Task<GetCourseResponse> GetByCollege(long collegeId, CancellationToken cancellationToken,
        long? universityId = null)
    {
        var collegeUniversityId = await db.Colleges.AsNoTracking()
            .Where(c => c.Id == collegeId)
            .Select(c => (long?)c.University.Id)
            .SingleOrDefaultAsync(cancellationToken);

        if (collegeUniversityId is null)
            return new GetCourseResponse(GetCourseResult.CollegeNotFound, PagedResponse<CourseResponse>.EmptyPage);

        if (universityId is { } scope && collegeUniversityId != scope)
            return new GetCourseResponse(GetCourseResult.CollegeInOtherUniversity,
                PagedResponse<CourseResponse>.EmptyPage);

        var items = await db.Courses.AsNoTracking()
            .Where(c => c.College != null && c.College.Id == collegeId)
            .OrderBy(c => c.Id)
            .Select(ToResponse)
            .ToListAsync(cancellationToken);

        return new GetCourseResponse(GetCourseResult.Success, ToPage(items));
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

    private static PagedResponse<CourseResponse> ToPage(IReadOnlyList<CourseResponse> items) =>
        new(items, Page: 1, PageSize: Math.Max(items.Count, 1), TotalCount: items.Count);
}