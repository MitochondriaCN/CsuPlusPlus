using CsuPlusPlus.Dtos.Common;

namespace CsuPlusPlus.Services.Course;

public interface ICourseService
{
    Task<GetCourseResponse> GetByUniversity(long universityId, PageRequest request,
        CancellationToken cancellationToken);

    Task<GetCourseResponse> GetByCollege(long collegeId, PageRequest request, CancellationToken cancellationToken,
        long? universityId = null);

    Task<GetSingleCourseResponse> GetById(long id, CancellationToken cancellationToken, long? universityId = null);
}