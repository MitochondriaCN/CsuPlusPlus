namespace CsuPlusPlus.Services.Release;

public enum DownloadLatestBuildStatus
{
    Success,
    NotFound,
    InvalidPlatform,
    StorageFailed
}