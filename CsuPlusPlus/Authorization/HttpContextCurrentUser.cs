using System.Globalization;
using System.Security.Claims;

namespace CsuPlusPlus.Authorization;

public class HttpContextCurrentUser(IHttpContextAccessor httpContextAccessor) : ICurrentUser
{
    private ClaimsPrincipal? Principal => httpContextAccessor.HttpContext?.User;

    public long? UserId => Principal?.GetUserId();

    public long? CollegeId => ReadId("college");

    public long? UniversityId => ReadId("university");

    private long? ReadId(string claimType) =>
        long.TryParse(Principal?.FindFirstValue(claimType), NumberStyles.Integer, CultureInfo.InvariantCulture,
            out var id) && id > 0
            ? id
            : null;
}