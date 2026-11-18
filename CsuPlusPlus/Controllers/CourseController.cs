using CsuPlusPlus.Authorization;
using CsuPlusPlus.Dtos.Common;
using CsuPlusPlus.Dtos.Course;
using CsuPlusPlus.Services.Course;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace CsuPlusPlus.Controllers;

[ApiController]
[Route("course")]
public class CourseController(ICourseService courseService, ICurrentUser currentUser) : ControllerBase
{
    [Authorize(Policy = Policies.AccessOwnUniversity)]
    [HttpGet("list")]
    public async Task<ActionResult<PagedResponse<CourseResponse>>> List(
        [FromQuery] PageRequest request,
        CancellationToken cancellationToken)
    {
        if (currentUser.UniversityId is not { } universityId)
            return Problem(statusCode: StatusCodes.Status401Unauthorized, title: "用户身份无效。");

        var result = await courseService.GetByUniversityAsync(universityId, request, cancellationToken);
        return result.Status switch
        {
            GetCourseStatus.Success => Ok(result.Response),
            GetCourseStatus.UniversityNotFound => Problem(
                statusCode: StatusCodes.Status404NotFound,
                title: "大学不存在。"),
            _ => throw new InvalidOperationException("未知结果。")
        };
    }

    [Authorize(Policy = Policies.AccessOwnUniversity)]
    [HttpGet("list/college")]
    public async Task<ActionResult<PagedResponse<CourseResponse>>> ListByCollege(
        [FromQuery] long collegeId,
        [FromQuery] PageRequest request,
        CancellationToken cancellationToken)
    {
        if (currentUser.UniversityId is not { } universityId)
            return Problem(statusCode: StatusCodes.Status401Unauthorized, title: "用户身份无效。");

        var result = await courseService.GetByCollegeAsync(collegeId, request, cancellationToken, universityId);
        return result.Status switch
        {
            GetCourseStatus.Success => Ok(result.Response),
            GetCourseStatus.CollegeNotFound => Problem(
                statusCode: StatusCodes.Status404NotFound,
                title: "学院不存在。"),
            GetCourseStatus.CollegeInOtherUniversity => Problem(
                statusCode: StatusCodes.Status403Forbidden,
                title: "只能查询本校学院的课程。"),
            _ => throw new InvalidOperationException("未知结果。")
        };
    }

    [Authorize(Policy = Policies.AccessOwnUniversity)]
    [HttpGet("{id:long}")]
    public async Task<ActionResult<CourseResponse>> GetById(
        [FromRoute] long id,
        CancellationToken cancellationToken)
    {
        if (currentUser.UniversityId is not { } universityId)
            return Problem(statusCode: StatusCodes.Status401Unauthorized, title: "用户身份无效。");

        var result = await courseService.GetByIdAsync(id, cancellationToken, universityId);
        return result.Status switch
        {
            GetCourseStatus.Success => Ok(result.Response),
            GetCourseStatus.CourseNotFound => Problem(
                statusCode: StatusCodes.Status404NotFound,
                title: "课程不存在。"),
            GetCourseStatus.CourseInOtherUniversity => Problem(
                statusCode: StatusCodes.Status403Forbidden,
                title: "只能查询本校课程。"),
            _ => throw new InvalidOperationException("未知结果。")
        };
    }

    [Authorize(Policy = Policies.ManageOwnUniversity)]
    [HttpPost("create")]
    public async Task<ActionResult<CourseResponse>> Create(
        [FromBody] CreateCourseRequest request, CancellationToken cancellationToken)
    {
        if (currentUser.UniversityId is not { } universityId)
            return Problem(statusCode: StatusCodes.Status401Unauthorized, title: "用户身份无效。");

        return MapCreateCourseResult(
            await courseService.CreateAsync(universityId, request, cancellationToken));
    }

    [Authorize(Policy = Policies.PlatformAdmin)]
    [HttpPost("create/any")]
    public async Task<ActionResult<CourseResponse>> CreateAny(
        [FromQuery] long universityId,
        [FromBody] CreateCourseRequest request,
        CancellationToken cancellationToken)
    {
        return MapCreateCourseResult(
            await courseService.CreateAsync(universityId, request, cancellationToken));
    }

    private ActionResult<CourseResponse> MapCreateCourseResult(CreateCourseResponse result) =>
        result.Status switch
        {
            CreateCourseStatus.Success => Created(uri: null as Uri, value: result.Response),
            CreateCourseStatus.UniversityNotFound => Problem(
                statusCode: StatusCodes.Status404NotFound,
                title: "大学不存在。"),
            CreateCourseStatus.CollegeNotFound => Problem(
                statusCode: StatusCodes.Status404NotFound,
                title: "学院不存在。"),
            CreateCourseStatus.CollegeInOtherUniversity => Problem(
                statusCode: StatusCodes.Status400BadRequest,
                title: "学院不属于该大学。"),
            _ => throw new InvalidOperationException("未知结果。")
        };
}