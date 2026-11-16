using CsuPlusPlus.Dtos.Common;
using CsuPlusPlus.Dtos.Course;
using CsuPlusPlus.Dtos.File;

namespace CsuPlusPlus.Services.File;

public interface ICourseFileService
{
    Task<BindCourseFileResponse> BindCourseFile(long courseId, long fileId, CancellationToken cancellationToken);

    Task<PagedResponse<FileMetadataResponse>> GetFilesBy(long courseId, PageRequest pageRequest,
        CancellationToken cancellationToken);

    Task<PagedResponse<CourseResponse>> GetCoursesBy(long fileId, PageRequest pageRequest,
        CancellationToken cancellationToken);
}