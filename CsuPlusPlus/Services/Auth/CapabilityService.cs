using System.Security.Claims;
using CsuPlusPlus.Authorization;
using CsuPlusPlus.Dtos.Auth;
using Microsoft.AspNetCore.Authorization;

namespace CsuPlusPlus.Services.Auth;

public class CapabilityService(IAuthorizationService authorizationService) : ICapabilityService
{
    private static readonly string[] ExposedPolicies =
    [
        Policies.AccessOwnUniversity,
        Policies.ManageOwnUniversity,
        Policies.PlatformAdmin
    ];

    public async Task<CapabilitiesResponse> GetCapabilitiesAsync(
        ClaimsPrincipal user,
        CancellationToken cancellationToken = default)
    {
        cancellationToken.ThrowIfCancellationRequested();

        var capabilities = new Dictionary<string, bool>(ExposedPolicies.Length, StringComparer.Ordinal);
        foreach (var policy in ExposedPolicies)
        {
            var result = await authorizationService.AuthorizeAsync(user, policy);
            capabilities[policy] = result.Succeeded;
        }

        return new CapabilitiesResponse(capabilities);
    }
}