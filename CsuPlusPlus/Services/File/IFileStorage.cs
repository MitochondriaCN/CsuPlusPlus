using CsuPlusPlus.Dtos.File;

namespace CsuPlusPlus.Services.File;

public interface IFileStorage
{
    Task UploadAsync(string objectKey, Stream content, string contentType, CancellationToken cancellationToken);

    Task<bool> ExistsAsync(string objectKey, CancellationToken cancellationToken);

    Task DeleteAsync(string objectKey, CancellationToken cancellationToken);

    DownloadFileUrlResponse CreatePresignedDownloadUrl(string objectKey, string downloadName, string contentType);
}