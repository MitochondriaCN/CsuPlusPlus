namespace CsuPlusPlus.Services.Course;

public interface ICourseService
{
    Task<GetCourseResponse> GetByUniversity(long universityId, CancellationToken cancellationToken);

    Task<GetCourseResponse> GetByCollege(long collegeId, CancellationToken cancellationToken,
        long? universityId = null);

    Task<GetSingleCourseResponse> GetById(long id, CancellationToken cancellationToken, long? universityId = null);
}