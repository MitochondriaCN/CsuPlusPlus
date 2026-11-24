using CsuPlusPlus.Dtos.Release;
using CsuPlusPlus.Services.Setting;
using CsuPlusPlus.Settings;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace CsuPlusPlus.Controllers;

[ApiController]
[Route("release")]
public class ReleaseController(ISettingsService settingsService) : ControllerBase
{
    [AllowAnonymous]
    [HttpGet]
    public async Task<ActionResult<ReleaseResponse>> Get(CancellationToken cancellationToken)
    {
        var release = await settingsService.GetAsync(
            SettingsKey.Release,
            ReleaseSettings.Default,
            cancellationToken);

        return Ok(new ReleaseResponse(
            release.CurrentBuildCode,
            release.CurrentBuildName,
            release.ReleaseUrl));
    }
}