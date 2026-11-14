using CsuPlusPlus.Dtos.File;

namespace CsuPlusPlus.Services.File;

public interface IFileService
{
    Task<FileMetadataResponse?> GetFileMetadataAsync(long id, CancellationToken cancellationToken);

    Task<CreateFileResponse> CreateFileAsync(CreateFileRequest request, long uploaderUserId,
        bool allowCrossUniversity, CancellationToken cancellationToken);

    Task<DownloadFileResponse> DownloadFileAsync(long id, long userId,
        bool allowCrossUniversity, CancellationToken cancellationToken);
}