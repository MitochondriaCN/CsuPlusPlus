using CsuPlusPlus.Authorization;
using CsuPlusPlus.Dtos.Auth;
using CsuPlusPlus.Services.UserProfile;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace CsuPlusPlus.Controllers;

[ApiController]
[Route("profile")]
public class UserProfileController(IUserProfileService userProfileService) : ControllerBase
{
    [Authorize]
    [HttpGet("me")]
    public async Task<ActionResult<UserProfileDto>> Me(CancellationToken cancellationToken)
    {
        var userId = User.GetUserId();
        if (userId is null)
            return Problem(statusCode: StatusCodes.Status401Unauthorized, title: "用户身份无效。");

        var profile = await userProfileService.GetProfileAsync(userId.Value, cancellationToken);
        if (profile is null)
            return Problem(statusCode: StatusCodes.Status404NotFound, title: "用户不存在。");

        return Ok(profile);
    }
}