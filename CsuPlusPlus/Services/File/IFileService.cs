using CsuPlusPlus.Dtos.File;

namespace CsuPlusPlus.Services.File;

public interface IFileService
{
    Task<FileMetadataResponse?> GetFileMetadata(long id, CancellationToken cancellationToken);
}