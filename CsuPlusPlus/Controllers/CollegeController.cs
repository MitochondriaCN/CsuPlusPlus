using CsuPlusPlus.Authorization;
using CsuPlusPlus.Dtos.College;
using CsuPlusPlus.Services.College;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace CsuPlusPlus.Controllers;

[ApiController]
[Route("college")]
public class CollegeController(ICollegeService collegeService) : ControllerBase
{
    [Authorize(Policies.ManageUniversity)]
    [HttpPost("create")]
    public async Task<ActionResult<CollegeResponse>> Create(
        [FromBody] CreateCollegeRequest request,
        CancellationToken cancellationToken)
    {
        var result = await collegeService.CreateCollegeAsync(request, cancellationToken);
        return result.Status switch
        {
            CreateCollegeStatus.Success => Created(uri: null as Uri, value: result.Response),
            CreateCollegeStatus.UniversityNotFound => Problem(
                statusCode: StatusCodes.Status404NotFound,
                title: "大学不存在。"),
            CreateCollegeStatus.NameConflictInUniversity => Problem(
                statusCode: StatusCodes.Status409Conflict,
                title: "该大学下学院名称已存在。"),
            _ => throw new InvalidOperationException("未知结果。")
        };
    }
}