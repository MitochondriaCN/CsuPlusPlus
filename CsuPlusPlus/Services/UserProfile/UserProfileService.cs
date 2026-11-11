using CsuPlusPlus.Dtos.Auth;
using CsuPlusPlus.Infra;
using Microsoft.EntityFrameworkCore;

namespace CsuPlusPlus.Services.UserProfile;

public class UserProfileService(
    AppDbContext db) : IUserProfileService
{
    public Task<UserProfileDto?> GetProfileAsync(long userId, CancellationToken cancellationToken = default) =>
        db.Users.AsNoTracking()
            .Where(u => u.Id == userId)
            .Select(u =>
                new UserProfileDto(u.Id, u.MailAddress.Value, u.Nickname.Value, u.College.Id, u.College.Name.Value))
            .SingleOrDefaultAsync(cancellationToken);
}