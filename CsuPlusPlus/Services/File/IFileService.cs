using CsuPlusPlus.Dtos.Common;
using CsuPlusPlus.Dtos.Course;
using CsuPlusPlus.Dtos.File;

namespace CsuPlusPlus.Services.File;

public interface IFileService
{
    Task<FileMetadataResponse?> GetFileMetadataAsync(long id, CancellationToken cancellationToken);

    /// <summary>
    /// 上传文件。courseId 为 null 时上传全局文件，否则绑定到课程。
    /// </summary>
    Task<CreateFileResponse> CreateFileAsync(CreateFileRequest request, long uploaderUserId, long? courseId,
        CancellationToken cancellationToken);

    Task<DownloadFileResponse> DownloadFileAsync(long id, CancellationToken cancellationToken);

    Task<DownloadFileResponse> DownloadCourseFileAsync(long id, long userId, CancellationToken cancellationToken);

    /// <summary>
    /// 把已存在的文件绑定到课程。
    /// </summary>
    Task<BindCourseFileResponse> BindCourseFileAsync(long courseId, long fileId, long userId,
        CancellationToken cancellationToken);

    Task<PagedResponse<FileMetadataResponse>> GetFilesBy(long courseId, PageRequest pageRequest,
        CancellationToken cancellationToken);

    Task<PagedResponse<FileMetadataResponse>> SearchFilesByAsync(long courseId, string keyword, PageRequest pageRequest,
        CancellationToken cancellationToken);

    Task<PagedResponse<CourseResponse>> GetCoursesBy(long fileId, PageRequest pageRequest,
        CancellationToken cancellationToken);
}