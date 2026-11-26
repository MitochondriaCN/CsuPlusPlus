using CsuPlusPlus.Dtos.Auth;
using CsuPlusPlus.Dtos.UserProfile;
using CsuPlusPlus.Infra;
using CsuPlusPlus.Models.ValueObjects;
using Microsoft.EntityFrameworkCore;

namespace CsuPlusPlus.Services.UserProfile;

public class UserProfileService(
    AppDbContext db) : IUserProfileService
{
    public Task<UserProfileDto?> GetProfileAsync(long userId, CancellationToken cancellationToken = default) =>
        db.Users.AsNoTracking()
            .Where(u => u.Id == userId)
            .Select(u =>
                new UserProfileDto(
                    u.Id,
                    u.MailAddress.Value,
                    u.Nickname.Value,
                    u.College.University.Id,
                    u.College.University.Name.Value,
                    u.College.Id,
                    u.College.Name.Value))
            .SingleOrDefaultAsync(cancellationToken);

    public async Task<UpdateProfileResult> UpdateProfileAsync(
        long userId,
        UpdateProfileRequest request,
        CancellationToken cancellationToken = default)
    {
        Nickname nickname;
        try
        {
            nickname = new Nickname(request.Nickname);
        }
        catch (ArgumentException)
        {
            return new UpdateProfileResult(UpdateProfileStatus.InvalidProfile);
        }

        var user = await db.Users
            .Include(u => u.College)
            .ThenInclude(c => c.University)
            .SingleOrDefaultAsync(u => u.Id == userId, cancellationToken);
        if (user is null)
            return new UpdateProfileResult(UpdateProfileStatus.UserNotFound);

        user.Nickname = nickname;
        await db.SaveChangesAsync(cancellationToken);

        return new UpdateProfileResult(
            UpdateProfileStatus.Success,
            new UserProfileDto(
                user.Id,
                user.MailAddress.Value,
                user.Nickname.Value,
                user.College.University.Id,
                user.College.University.Name.Value,
                user.College.Id,
                user.College.Name.Value));
    }
}