using System.Text.Json;
using CsuPlusPlus.Authorization;
using CsuPlusPlus.Dtos.Setting;
using CsuPlusPlus.Services.Setting;
using CsuPlusPlus.Settings;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace CsuPlusPlus.Controllers;

[ApiController]
[Route("settings")]
public class SettingsController(ISettingsService settingsService) : ControllerBase
{
    [Authorize(Policies.ManageSettings)]
    [HttpGet("{key}")]
    public async Task<ActionResult<SettingResponse>> Get(
        [FromRoute] SettingsKey key,
        CancellationToken cancellationToken)
    {
        var value = await settingsService.GetWithDefaultAsync(key, cancellationToken);
        return Ok(new SettingResponse(key.ToString(), value));
    }

    [Authorize(Policies.ManageSettings)]
    [HttpPut("{key}")]
    public async Task<ActionResult<SettingResponse>> Set(
        [FromRoute] SettingsKey key,
        [FromBody] UpdateSettingRequest request,
        CancellationToken cancellationToken)
    {
        if (request.Value.ValueKind != JsonValueKind.Object)
            return Problem(statusCode: StatusCodes.Status400BadRequest, title: "配置值必须是 JSON 对象。");

        await settingsService.SetAsync(key, request.Value, cancellationToken);

        var value = await settingsService.GetWithDefaultAsync(key, cancellationToken);
        return Ok(new SettingResponse(key.ToString(), value));
    }
}