using CsuPlusPlus.Authorization;
using CsuPlusPlus.Dtos.University;
using CsuPlusPlus.Services.University;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace CsuPlusPlus.Controllers;

[ApiController]
[Route("university")]
public class UniversityController(IUniversityService universityService) : ControllerBase
{
    [Authorize(Policies.ManageUniversity)]
    [HttpPost("create")]
    public async Task<ActionResult<UniversityResponse>> Create(
        [FromBody] CreateUniversityRequest request,
        CancellationToken cancellationToken)
    {
        var result = await universityService.CreateUniversityAsync(request, cancellationToken);
        return result.Status switch
        {
            CreateUniversityStatus.Success => Created(uri: null as Uri, value: result.Response),
            CreateUniversityStatus.NameAlreadyExists => Problem(
                statusCode: StatusCodes.Status409Conflict,
                title: "大学名称已存在。"),
            _ => throw new InvalidOperationException("未知结果。")
        };
    }
}