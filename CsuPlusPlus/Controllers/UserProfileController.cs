using CsuPlusPlus.Authorization;
using CsuPlusPlus.Dtos.Auth;
using CsuPlusPlus.Dtos.UserProfile;
using CsuPlusPlus.Services.UserProfile;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace CsuPlusPlus.Controllers;

[ApiController]
[Route("profile")]
public class UserProfileController(IUserProfileService userProfileService, ICurrentUser currentUser)
    : ControllerBase
{
    [Authorize]
    [HttpGet("me")]
    public async Task<ActionResult<UserProfileDto>> Me(CancellationToken cancellationToken)
    {
        if (currentUser.UserId is not { } userId)
            return Problem(statusCode: StatusCodes.Status401Unauthorized, title: "用户身份无效。");

        var profile = await userProfileService.GetProfileAsync(userId, cancellationToken);
        if (profile is null)
            return Problem(statusCode: StatusCodes.Status404NotFound, title: "用户不存在。");

        return Ok(profile);
    }

    [Authorize]
    [HttpPost("me")]
    public async Task<ActionResult<UserProfileDto>> UpdateMe(
        [FromBody] UpdateProfileRequest request,
        CancellationToken cancellationToken)
    {
        if (currentUser.UserId is not { } userId)
            return Problem(statusCode: StatusCodes.Status401Unauthorized, title: "用户身份无效。");

        var result = await userProfileService.UpdateProfileAsync(userId, request, cancellationToken);
        return result.Status switch
        {
            UpdateProfileStatus.Success => Ok(result.Response),
            UpdateProfileStatus.UserNotFound => Problem(
                statusCode: StatusCodes.Status404NotFound,
                title: "用户不存在。"),
            UpdateProfileStatus.InvalidProfile => Problem(
                statusCode: StatusCodes.Status400BadRequest,
                title: "昵称格式无效。"),
            _ => throw new InvalidOperationException("未知结果。")
        };
    }
}