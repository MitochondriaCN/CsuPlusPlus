using CsuPlusPlus.Dtos.Common;
using CsuPlusPlus.Dtos.Course;

namespace CsuPlusPlus.Services.Course;

public interface ICourseService
{
    Task<GetCourseResponse> GetByUniversityAsync(long universityId, PageRequest request,
        CancellationToken cancellationToken, long? collegeId = null);

    Task<GetCourseResponse> SearchAsync(long universityId, string? keyword, PageRequest request,
        CancellationToken cancellationToken);

    Task<GetCourseResponse> GetByCollegeAsync(long collegeId, PageRequest request, CancellationToken cancellationToken,
        long? universityId = null);

    Task<GetSingleCourseResponse> GetByIdAsync(long id, CancellationToken cancellationToken,
        long? universityId = null);

    Task<CreateCourseResponse> CreateAsync(long universityId, CreateCourseRequest request,
        CancellationToken cancellationToken);

    Task<DeleteCourseResponse> DeleteAsync(long id, CancellationToken cancellationToken,
        long? universityId = null);
}