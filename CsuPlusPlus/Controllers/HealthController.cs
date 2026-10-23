using CsuPlusPlus.Dtos.Health;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace CsuPlusPlus.Controllers;

[ApiController]
[Route("health")]
public class HealthController : ControllerBase
{
    [AllowAnonymous]
    [HttpGet("check")]
    public ActionResult<HealthCheckResponse> Check()
    {
        return Ok(new HealthCheckResponse(DateTimeOffset.Now));
    }
}