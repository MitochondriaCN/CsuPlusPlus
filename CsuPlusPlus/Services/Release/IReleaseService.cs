using CsuPlusPlus.Dtos.Release;

namespace CsuPlusPlus.Services.Release;

public interface IReleaseService
{
    Task<DownloadLatestBuildResult> DownloadLatestAsync(
        ReleasePlatform platform,
        CancellationToken cancellationToken);
}