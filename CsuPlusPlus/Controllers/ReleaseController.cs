using System.ComponentModel.DataAnnotations;
using CsuPlusPlus.Dtos.Release;
using CsuPlusPlus.Services.Release;
using CsuPlusPlus.Services.Setting;
using CsuPlusPlus.Settings;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.RateLimiting;

namespace CsuPlusPlus.Controllers;

[ApiController]
[Route("release")]
public class ReleaseController(ISettingsService settingsService, IReleaseService releaseService) : ControllerBase
{
    [AllowAnonymous]
    [HttpGet]
    public async Task<ActionResult<ReleaseResponse>> Get(CancellationToken cancellationToken)
    {
        var release = await settingsService.GetAsync(
            SettingsKey.Release,
            ReleaseSettings.Default,
            cancellationToken);

        return Ok(new ReleaseResponse(
            release.CurrentBuildCode,
            release.CurrentBuildName,
            release.ReleaseUrl));
    }

    [AllowAnonymous]
    [EnableRateLimiting(RateLimitPolicies.FileDownload)]
    [HttpGet("download")]
    public async Task<IActionResult> DownloadLatest(
        [FromQuery, Required(ErrorMessage = "请选择平台。")]
        ReleasePlatform? platform,
        CancellationToken cancellationToken)
    {
        var result = await releaseService.DownloadLatestAsync(platform!.Value, cancellationToken);
        return result.Status switch
        {
            DownloadLatestBuildStatus.Success => Redirect(result.Url!),
            DownloadLatestBuildStatus.NotFound => Problem(
                statusCode: StatusCodes.Status404NotFound,
                title: "该平台暂无可用构建。"),
            DownloadLatestBuildStatus.InvalidPlatform => Problem(
                statusCode: StatusCodes.Status400BadRequest,
                title: "不支持的平台。"),
            DownloadLatestBuildStatus.StorageFailed => Problem(
                statusCode: StatusCodes.Status502BadGateway,
                title: "文件存储服务异常，请稍后重试。"),
            _ => throw new InvalidOperationException("未知结果。")
        };
    }
}