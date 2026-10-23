using CsuPlusPlus.Models;

namespace CsuPlusPlus.Dtos.Auth;

public record UserProfileDto(long Id, string Email, string Nickname, long CollegeId)
{
    public static UserProfileDto From(User user) =>
        new(user.Id, user.MailAddress.Value, user.Nickname.Value, user.College.Id);
}