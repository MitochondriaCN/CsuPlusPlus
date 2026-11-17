using CsuPlusPlus.Dtos.File;

namespace CsuPlusPlus.Services.File;

public record GetFileMetadataResponse(
    GetFileMetadataResult Result,
    FileMetadataResponse? Response = null
);

public enum GetFileMetadataResult
{
    Success,
    NotFound,
    Forbidden
}