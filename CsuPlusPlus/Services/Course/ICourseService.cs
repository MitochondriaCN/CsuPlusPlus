using CsuPlusPlus.Dtos.Common;

namespace CsuPlusPlus.Services.Course;

public interface ICourseService
{
    Task<GetCourseResponse> GetByUniversityAsync(long universityId, PageRequest request,
        CancellationToken cancellationToken);

    Task<GetCourseResponse> GetByCollegeAsync(long collegeId, PageRequest request, CancellationToken cancellationToken,
        long? universityId = null);

    Task<GetSingleCourseResponse> GetByIdAsync(long id, CancellationToken cancellationToken,
        long? universityId = null);
}