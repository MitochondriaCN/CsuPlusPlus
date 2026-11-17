using CsuPlusPlus.Dtos.Common;
using CsuPlusPlus.Dtos.Course;
using CsuPlusPlus.Dtos.File;

namespace CsuPlusPlus.Services.File;

public interface IFileService
{
    Task<GetFileMetadataResponse> GetFileMetadataAsync(long id, CancellationToken cancellationToken,
        long? universityId = null);

    Task<CreateFileResponse> CreateFileAsync(CreateFileRequest request, long uploaderUserId,
        CancellationToken cancellationToken);

    /// <summary>
    /// 上传文件并绑定到课程，课程必须属于 universityId 对应的大学。
    /// </summary>
    Task<CreateFileResponse> CreateCourseFileAsync(CreateFileRequest request, long uploaderUserId, long courseId,
        long universityId, CancellationToken cancellationToken);

    Task<DownloadFileResponse> DownloadFileAsync(long id, CancellationToken cancellationToken);

    Task<DownloadFileResponse> DownloadCourseFileAsync(long id, long universityId,
        CancellationToken cancellationToken);

    /// <summary>
    /// 把已存在的文件绑定到课程，课程必须属于 universityId 对应的大学。
    /// </summary>
    Task<BindCourseFileResponse> BindCourseFileAsync(long courseId, long fileId, long universityId,
        CancellationToken cancellationToken);

    Task<PagedResponse<FileMetadataResponse>> GetFilesBy(long universityId, long courseId, PageRequest pageRequest,
        CancellationToken cancellationToken);

    Task<PagedResponse<FileMetadataResponse>> SearchFilesByAsync(long universityId, long courseId, string keyword,
        PageRequest pageRequest, CancellationToken cancellationToken);

    Task<PagedResponse<CourseResponse>> GetCoursesBy(long fileId, PageRequest pageRequest,
        CancellationToken cancellationToken);
}