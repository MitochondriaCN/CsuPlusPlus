using CsuPlusPlus.Dtos.Release;
using CsuPlusPlus.Services.File;

namespace CsuPlusPlus.Services.Release;

public class ReleaseService(IFileStorage fileStorage, ILogger<ReleaseService> logger) : IReleaseService
{
    private const string ObjectKeyPrefix = "releases/";

    public async Task<DownloadLatestBuildResult> DownloadLatestAsync(
        ReleasePlatform platform,
        CancellationToken cancellationToken)
    {
        var package = GetPackageInfo(platform);
        if (package is null)
            return new DownloadLatestBuildResult(DownloadLatestBuildStatus.InvalidPlatform);

        var (fileName, contentType) = package.Value;
        var objectKey = ObjectKeyPrefix + fileName;

        bool exists;
        try
        {
            exists = await fileStorage.ExistsAsync(objectKey, cancellationToken);
        }
        catch (Exception ex) when (ex is not OperationCanceledException)
        {
            logger.LogError(ex, "检查最新构建对象是否存在失败：{ObjectKey}", objectKey);
            return new DownloadLatestBuildResult(DownloadLatestBuildStatus.StorageFailed);
        }

        if (!exists)
            return new DownloadLatestBuildResult(DownloadLatestBuildStatus.NotFound);

        var download = fileStorage.CreatePresignedDownloadUrl(objectKey, fileName, contentType);
        return new DownloadLatestBuildResult(DownloadLatestBuildStatus.Success, download.Url);
    }

    private static (string FileName, string ContentType)? GetPackageInfo(ReleasePlatform platform) =>
        platform switch
        {
            ReleasePlatform.Android => ("latest_build.apk", "application/vnd.android.package-archive"),
            ReleasePlatform.MacOs => ("latest_build.dmg", "application/x-apple-diskimage"),
            ReleasePlatform.Windows => ("latest_build.exe", "application/octet-stream"),
            ReleasePlatform.Linux => ("latest_build.AppImage", "application/octet-stream"),
            ReleasePlatform.Ios => ("latest_build.ipa", "application/octet-stream"),
            _ => null
        };
}