using CsuPlusPlus.Authorization;
using CsuPlusPlus.Dtos.Common;
using CsuPlusPlus.Dtos.File;
using CsuPlusPlus.Services.File;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.RateLimiting;

namespace CsuPlusPlus.Controllers;

[ApiController]
[Route("file")]
public class FileController(IFileService fileService, ICurrentUser currentUser) : ControllerBase
{
    [Authorize(Policy = Policies.AccessOwnUniversity)]
    [HttpGet]
    public async Task<ActionResult<FileMetadataResponse>> GetFileMetadata(
        long id, CancellationToken cancellationToken)
    {
        if (currentUser.UniversityId is not { } universityId)
            return Problem(statusCode: StatusCodes.Status401Unauthorized, title: "用户身份无效。");

        var response = await fileService.GetFileMetadataAsync(id, cancellationToken, universityId);
        return response.Status switch
        {
            GetFileMetadataStatus.Success => Ok(response.Response),
            GetFileMetadataStatus.NotFound => Problem(statusCode: StatusCodes.Status404NotFound, title: "文件未找到。"),
            GetFileMetadataStatus.Forbidden => Problem(statusCode: StatusCodes.Status403Forbidden, title: "您无权限访问此文件。"),
            _ => throw new InvalidOperationException("未知错误。")
        };
    }

    [Authorize(Policy = Policies.PlatformAdmin)]
    [HttpPost("upload")]
    [Consumes("multipart/form-data")]
    public async Task<ActionResult<FileMetadataResponse>> UploadFile(
        [FromForm] CreateFileRequest request,
        CancellationToken cancellationToken)
    {
        if (currentUser.UserId is not { } userId)
            return Problem(statusCode: StatusCodes.Status401Unauthorized, title: "用户身份无效。");

        return MapCreateFileResult(
            await fileService.CreateFileAsync(request, userId, cancellationToken));
    }

    [Authorize(Policy = Policies.AccessOwnUniversity)]
    [HttpPost("upload/course")]
    [Consumes("multipart/form-data")]
    public async Task<ActionResult<FileMetadataResponse>> UploadCourseFile(
        [FromForm] CreateFileRequest request, long courseId,
        CancellationToken cancellationToken)
    {
        if (currentUser is not { UserId: { } userId, UniversityId: { } universityId })
            return Problem(statusCode: StatusCodes.Status401Unauthorized, title: "用户身份无效。");

        return MapCreateFileResult(
            await fileService.CreateCourseFileAsync(request, userId, courseId, universityId,
                cancellationToken));
    }

    [Authorize(Policy = Policies.AccessOwnUniversity)]
    [HttpGet("list/course")]
    public async Task<ActionResult<PagedResponse<FileMetadataResponse>>> ListCourseFiles(
        [FromQuery] long courseId,
        [FromQuery] PageRequest request,
        CancellationToken cancellationToken)
    {
        if (currentUser.UniversityId is not { } universityId)
            return Problem(statusCode: StatusCodes.Status401Unauthorized, title: "用户身份无效。");

        return Ok(await fileService.GetFilesBy(universityId, courseId, request, cancellationToken));
    }

    [Authorize(Policy = Policies.AccessOwnUniversity)]
    [HttpGet("search/course")]
    public async Task<ActionResult<PagedResponse<FileMetadataResponse>>> SearchCourseFiles(
        [FromQuery] long courseId,
        [FromQuery] string keyword,
        [FromQuery] PageRequest request,
        CancellationToken cancellationToken)
    {
        if (currentUser.UniversityId is not { } universityId)
            return Problem(statusCode: StatusCodes.Status401Unauthorized, title: "用户身份无效。");

        return Ok(await fileService.SearchFilesByAsync(universityId, courseId, keyword, request,
            cancellationToken));
    }

    [Authorize(Policy = Policies.AccessOwnUniversity)]
    [HttpPost("bind/course")]
    public async Task<ActionResult<CourseFileResponse>> BindCourseFile(
        [FromQuery] long courseId,
        [FromQuery] long fileId,
        CancellationToken cancellationToken)
    {
        if (currentUser.UniversityId is not { } universityId)
            return Problem(statusCode: StatusCodes.Status401Unauthorized, title: "用户身份无效。");

        var result = await fileService.BindCourseFileAsync(courseId, fileId, universityId, cancellationToken);
        return result.Status switch
        {
            BindCourseFileStatus.Success => Ok(result.Response),
            BindCourseFileStatus.FileNotFound => Problem(statusCode: StatusCodes.Status404NotFound,
                title: "文件不存在。"),
            BindCourseFileStatus.CourseNotFound => Problem(statusCode: StatusCodes.Status404NotFound,
                title: "课程不存在。"),
            BindCourseFileStatus.CourseInOtherUniversity => Problem(statusCode: StatusCodes.Status403Forbidden,
                title: "只能绑定本校课程。"),
            BindCourseFileStatus.Duplicate => Problem(statusCode: StatusCodes.Status409Conflict,
                title: "文件已绑定到课程。"),
            _ => throw new InvalidOperationException("未知结果。")
        };
    }

    private ActionResult MapCreateFileResult(CreateFileResponse result) =>
        result.Status == CreateFileStatus.Success
            ? Created(uri: null as Uri, value: result.Response)
            : MapCreateFileFailure(result);

    private ActionResult MapCreateFileFailure(CreateFileResponse result)
    {
        return result.Status switch
        {
            CreateFileStatus.TooLarge => Problem(
                statusCode: StatusCodes.Status413PayloadTooLarge,
                title: "文件超出大小限制。"),
            CreateFileStatus.FormatNotSupported => Problem(
                statusCode: StatusCodes.Status415UnsupportedMediaType,
                title: "不支持的文件格式。"),
            CreateFileStatus.CourseNotFound => Problem(
                statusCode: StatusCodes.Status404NotFound,
                title: "课程不存在。"),
            CreateFileStatus.CourseInOtherUniversity => Problem(
                statusCode: StatusCodes.Status403Forbidden,
                title: "只能向本校课程上传文件。"),
            CreateFileStatus.Duplicate => Problem(
                statusCode: StatusCodes.Status409Conflict,
                title: "文件已存在。",
                extensions: new Dictionary<string, object?> { ["existingFile"] = result.Response }),
            CreateFileStatus.DailyUploadLimitExceeded => Problem(
                statusCode: StatusCodes.Status429TooManyRequests,
                title: "今日上传次数已达上限。"),
            CreateFileStatus.UserNotFound => Problem(
                statusCode: StatusCodes.Status401Unauthorized,
                title: "用户不存在。"),
            _ => throw new InvalidOperationException("未知结果。")
        };
    }

    [Authorize(Policy = Policies.PlatformAdmin)]
    [EnableRateLimiting(RateLimitPolicies.FileDownload)]
    [HttpGet("download/{id:long}")]
    public async Task<IActionResult> DownloadFile(
        [FromRoute] long id,
        CancellationToken cancellationToken) =>
        MapDownloadResult(await fileService.DownloadFileAsync(id, cancellationToken));

    [Authorize(Policy = Policies.AccessOwnUniversity)]
    [EnableRateLimiting(RateLimitPolicies.FileDownload)]
    [HttpGet("download/course/{id:long}")]
    public async Task<IActionResult> DownloadCourseFile(
        [FromRoute] long id,
        CancellationToken cancellationToken)
    {
        if (currentUser.UniversityId is not { } universityId)
            return Problem(statusCode: StatusCodes.Status401Unauthorized, title: "用户身份无效。");

        return MapDownloadResult(
            await fileService.DownloadCourseFileAsync(id, universityId, cancellationToken));
    }

    private IActionResult MapDownloadResult(DownloadFileResponse result)
    {
        return result.Status switch
        {
            DownloadFileStatus.Success => File(
                result.Content!,
                result.ContentType!,
                result.DownloadName!),
            DownloadFileStatus.NotFound => Problem(
                statusCode: StatusCodes.Status404NotFound,
                title: "文件不存在。"),
            DownloadFileStatus.Forbidden => Problem(
                statusCode: StatusCodes.Status403Forbidden,
                title: "只能下载本校资料。"),
            DownloadFileStatus.ContentMissing => Problem(
                statusCode: StatusCodes.Status410Gone,
                title: "文件内容已丢失。"),
            _ => throw new InvalidOperationException("未知结果。")
        };
    }
}