namespace CsuPlusPlus.Services.Release;

public record DownloadLatestBuildResult(
    DownloadLatestBuildStatus Status,
    string? Url = null
);