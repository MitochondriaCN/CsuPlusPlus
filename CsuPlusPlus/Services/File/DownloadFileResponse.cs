namespace CsuPlusPlus.Services.File;

public record DownloadFileResponse(
    DownloadFileStatus Status,
    Stream? Content = null,
    string? ContentType = null,
    string? DownloadName = null
);

public enum DownloadFileStatus
{
    Success,
    NotFound,
    Forbidden,
    ContentMissing
}