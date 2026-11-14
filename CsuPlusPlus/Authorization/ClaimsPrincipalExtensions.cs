using System.Security.Claims;

namespace CsuPlusPlus.Authorization;

public static class ClaimsPrincipalExtensions
{
    /// <summary>
    /// 从当前登录用户的 Claims 中读取用户 ID；缺少 sub 声明或取值非法时返回 null。
    /// </summary>
    public static long? GetUserId(this ClaimsPrincipal principal)
    {
        var subject = principal.FindFirstValue(ClaimTypes.NameIdentifier) ?? principal.FindFirstValue("sub");
        return long.TryParse(subject, out var userId) && userId > 0 ? userId : null;
    }
}