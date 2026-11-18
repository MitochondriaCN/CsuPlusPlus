using CsuPlusPlus.Dtos.File;

namespace CsuPlusPlus.Services.File;

public record GetFileMetadataResponse(
    GetFileMetadataStatus Status,
    FileMetadataResponse? Response = null
);

public enum GetFileMetadataStatus
{
    Success,
    NotFound,
    Forbidden
}