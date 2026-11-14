using CsuPlusPlus.Authorization;
using CsuPlusPlus.Dtos.File;
using CsuPlusPlus.Services.File;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.RateLimiting;

namespace CsuPlusPlus.Controllers;

[ApiController]
[Route("file")]
public class FileController(IFileService fileService) : ControllerBase
{
    [Authorize(Policy = Policies.UploadGlobalFile)]
    [HttpPost("upload")]
    [Consumes("multipart/form-data")]
    public async Task<ActionResult<FileMetadataResponse>> UploadFile(
        [FromForm] CreateFileRequest request,
        CancellationToken cancellationToken)
    {
        var userId = User.GetUserId();
        if (userId is null)
            return Problem(statusCode: StatusCodes.Status401Unauthorized, title: "用户身份无效。");

        return await UploadFileCoreAsync(
            request: request,
            userId: userId.Value,
            allowCrossUniversity: true,
            cancellationToken: cancellationToken);
    }

    [Authorize]
    [HttpPost("upload/course")]
    [Consumes("multipart/form-data")]
    public async Task<ActionResult<FileMetadataResponse>> UploadCourseFile(
        [FromForm] CreateFileRequest request,
        CancellationToken cancellationToken)
    {
        if (request.CourseId == null)
            return Problem(statusCode: StatusCodes.Status400BadRequest, title: "必须指定课程。");

        var userId = User.GetUserId();
        if (userId is null)
            return Problem(statusCode: StatusCodes.Status401Unauthorized, title: "用户身份无效。");

        return await UploadFileCoreAsync(
            request: request,
            userId: userId.Value,
            allowCrossUniversity: false,
            cancellationToken: cancellationToken);
    }

    private async Task<ActionResult<FileMetadataResponse>> UploadFileCoreAsync(
        CreateFileRequest request,
        long userId,
        bool allowCrossUniversity,
        CancellationToken cancellationToken)
    {
        var result = await fileService.CreateFileAsync(request, userId, allowCrossUniversity, cancellationToken);
        return result.Result switch
        {
            CreateFileResult.Success => Created(uri: null as Uri, value: result.Response),
            CreateFileResult.TooLarge => Problem(
                statusCode: StatusCodes.Status413PayloadTooLarge,
                title: "文件超出大小限制。"),
            CreateFileResult.FormatNotSupported => Problem(
                statusCode: StatusCodes.Status415UnsupportedMediaType,
                title: "不支持的文件格式。"),
            CreateFileResult.CourseNotFound => Problem(
                statusCode: StatusCodes.Status404NotFound,
                title: "课程不存在。"),
            CreateFileResult.CourseInOtherUniversity => Problem(
                statusCode: StatusCodes.Status403Forbidden,
                title: "只能向本校课程上传文件。"),
            CreateFileResult.Duplicate => Problem(
                statusCode: StatusCodes.Status409Conflict,
                title: "文件已存在。",
                extensions: new Dictionary<string, object?> { ["existingFile"] = result.Response }),
            CreateFileResult.DailyUploadLimitExceeded => Problem(
                statusCode: StatusCodes.Status429TooManyRequests,
                title: "今日上传次数已达上限。"),
            CreateFileResult.UserNotFound => Problem(
                statusCode: StatusCodes.Status401Unauthorized,
                title: "用户不存在。"),
            _ => throw new InvalidOperationException("未知结果。")
        };
    }

    [Authorize(Policy = Policies.DownloadGlobalFile)]
    [EnableRateLimiting(RateLimitPolicies.FileDownload)]
    [HttpGet("download/{id:long}")]
    public async Task<IActionResult> DownloadFile(
        [FromRoute] long id,
        CancellationToken cancellationToken) =>
        await DownloadFileCoreAsync(id, allowCrossUniversity: true, cancellationToken);

    [Authorize]
    [EnableRateLimiting(RateLimitPolicies.FileDownload)]
    [HttpGet("download/course/{id:long}")]
    public async Task<IActionResult> DownloadCourseFile(
        [FromRoute] long id,
        CancellationToken cancellationToken) =>
        await DownloadFileCoreAsync(id, allowCrossUniversity: false, cancellationToken);

    private async Task<IActionResult> DownloadFileCoreAsync(
        long id,
        bool allowCrossUniversity,
        CancellationToken cancellationToken)
    {
        var userId = User.GetUserId();
        if (userId is null)
            return Problem(statusCode: StatusCodes.Status401Unauthorized, title: "用户身份无效。");

        var result = await fileService.DownloadFileAsync(id, userId.Value, allowCrossUniversity, cancellationToken);
        return result.Result switch
        {
            DownloadFileResult.Success => File(
                result.Content!,
                result.ContentType!,
                result.DownloadName!),
            DownloadFileResult.NotFound => Problem(
                statusCode: StatusCodes.Status404NotFound,
                title: "文件不存在。"),
            DownloadFileResult.Forbidden => Problem(
                statusCode: StatusCodes.Status403Forbidden,
                title: "只能下载本校资料。"),
            DownloadFileResult.ContentMissing => Problem(
                statusCode: StatusCodes.Status410Gone,
                title: "文件内容已丢失。"),
            _ => throw new InvalidOperationException("未知结果。")
        };
    }
}