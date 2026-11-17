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
    [Authorize(Policy = Policies.AccessOwnUniversityFiles)]
    [HttpGet("list")]
    public async Task<ActionResult<PagedResponse<CourseResponse>>> List(
        [FromQuery] PageRequest request,
        CancellationToken cancellationToken)
    {
        if (currentUser.UniversityId is not { } universityId)
            return Problem(statusCode: StatusCodes.Status401Unauthorized, title: "用户身份无效。");

        var result = await courseService.GetByUniversity(universityId, request, cancellationToken);
        return result.Result switch
        {
            GetCourseResult.Success => Ok(result.Response),
            GetCourseResult.UniversityNotFound => Problem(
                statusCode: StatusCodes.Status404NotFound,
                title: "大学不存在。"),
            _ => throw new InvalidOperationException("未知结果。")
        };
    }

    [Authorize(Policy = Policies.AccessOwnUniversityFiles)]
    [HttpGet("list/college")]
    public async Task<ActionResult<PagedResponse<CourseResponse>>> ListByCollege(
        [FromQuery] long collegeId,
        [FromQuery] PageRequest request,
        CancellationToken cancellationToken)
    {
        if (currentUser.UniversityId is not { } universityId)
            return Problem(statusCode: StatusCodes.Status401Unauthorized, title: "用户身份无效。");

        var result = await courseService.GetByCollege(collegeId, request, cancellationToken, universityId);
        return result.Result switch
        {
            GetCourseResult.Success => Ok(result.Response),
            GetCourseResult.CollegeNotFound => Problem(
                statusCode: StatusCodes.Status404NotFound,
                title: "学院不存在。"),
            GetCourseResult.CollegeInOtherUniversity => Problem(
                statusCode: StatusCodes.Status403Forbidden,
                title: "只能查询本校学院的课程。"),
            _ => throw new InvalidOperationException("未知结果。")
        };
    }

    [Authorize(Policy = Policies.AccessOwnUniversityFiles)]
    [HttpGet("{id:long}")]
    public async Task<ActionResult<CourseResponse>> GetById(
        [FromRoute] long id,
        CancellationToken cancellationToken)
    {
        if (currentUser.UniversityId is not { } universityId)
            return Problem(statusCode: StatusCodes.Status401Unauthorized, title: "用户身份无效。");

        var result = await courseService.GetById(id, cancellationToken, universityId);
        return result.Result switch
        {
            GetCourseResult.Success => Ok(result.Response),
            GetCourseResult.CourseNotFound => Problem(
                statusCode: StatusCodes.Status404NotFound,
                title: "课程不存在。"),
            GetCourseResult.CourseInOtherUniversity => Problem(
                statusCode: StatusCodes.Status403Forbidden,
                title: "只能查询本校课程。"),
            _ => throw new InvalidOperationException("未知结果。")
        };
    }
}