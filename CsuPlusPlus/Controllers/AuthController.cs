using CsuPlusPlus.Dtos.Auth;
using CsuPlusPlus.Services.Auth;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace CsuPlusPlus.Controllers;

[ApiController]
[Route("auth")]
public class AuthController(IAuthService authService) : ControllerBase
{
    [AllowAnonymous]
    [HttpPost("register")]
    public async Task<ActionResult<LoginResponse>> Register(
        RegisterRequest request, CancellationToken cancellationToken)
    {
        var result = await authService.RegisterAsync(request, cancellationToken);
        return result.Status switch
        {
            RegisterStatus.Success => Ok(result.Response),
            RegisterStatus.EmailAlreadyExists => Problem(
                statusCode: StatusCodes.Status409Conflict, title: "邮箱已注册。"),
            RegisterStatus.CollegeNotFound => Problem(
                statusCode: StatusCodes.Status400BadRequest, title: "学院不存在。"),
            RegisterStatus.InvalidProfile => Problem(
                statusCode: StatusCodes.Status400BadRequest, title: "邮箱或昵称格式无效。"),
            _ => throw new InvalidOperationException("未知注册结果。")
        };
    }

    [AllowAnonymous]
    [HttpPost("login")]
    public async Task<ActionResult<LoginResponse>> Login(
        LoginRequest request, CancellationToken cancellationToken)
    {
        var response = await authService.LoginAsync(request, cancellationToken);
        if (response is null)
            return Problem(statusCode: StatusCodes.Status401Unauthorized, title: "邮箱或密码错误。");

        return Ok(response);
    }
}