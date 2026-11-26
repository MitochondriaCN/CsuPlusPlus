using CsuPlusPlus.Dtos.Common;
using CsuPlusPlus.Dtos.Course;

namespace CsuPlusPlus.Services.Course;

public interface ICourseService
{
    Task<GetCourseResponse> ListAsync(
        PageRequest request,
        CancellationToken cancellationToken,
        long? universityId = null,
        long? collegeId = null,
        string? keyword = null);

    Task<GetSingleCourseResponse> GetByIdAsync(long id, CancellationToken cancellationToken,
        long? universityId = null);

    Task<CreateCourseResponse> CreateAsync(long universityId, CreateCourseRequest request,
        CancellationToken cancellationToken);

    Task<DeleteCourseResponse> DeleteAsync(long id, CancellationToken cancellationToken,
        long? universityId = null);
}