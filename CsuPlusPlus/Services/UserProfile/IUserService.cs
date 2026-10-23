using CsuPlusPlus.Dtos.Auth;

namespace CsuPlusPlus.Services.UserProfile;

public interface IUserProfileService
{
    Task<UserProfileDto?> GetProfileAsync(long userId, CancellationToken cancellationToken = default);
}