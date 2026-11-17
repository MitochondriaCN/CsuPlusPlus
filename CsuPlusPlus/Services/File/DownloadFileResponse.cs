namespace CsuPlusPlus.Services.File;

public record DownloadFileResponse(
    DownloadFileResult Result,
    Stream? Content = null,
    string? ContentType = null,
    string? DownloadName = null
);

public enum DownloadFileResult
{
    Success,
    NotFound,
    Forbidden,
    ContentMissing
}