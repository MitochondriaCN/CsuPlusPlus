using System.Security.Claims;
using CsuPlusPlus.Dtos.Auth;

namespace CsuPlusPlus.Services.Auth;

public interface ICapabilityService
{
    Task<CapabilitiesResponse> GetCapabilitiesAsync(
        ClaimsPrincipal user,
        CancellationToken cancellationToken = default);
}