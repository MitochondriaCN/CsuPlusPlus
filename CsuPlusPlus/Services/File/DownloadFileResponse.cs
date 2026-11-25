using CsuPlusPlus.Dtos.File;

namespace CsuPlusPlus.Services.File;

public record DownloadFileResponse(
    DownloadFileStatus Status,
    DownloadFileUrlResponse? Response = null
);

public enum DownloadFileStatus
{
    Success,
    NotFound,
    Forbidden
}
