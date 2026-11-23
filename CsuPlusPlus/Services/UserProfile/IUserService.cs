using CsuPlusPlus.Dtos.Auth;
using CsuPlusPlus.Dtos.UserProfile;

namespace CsuPlusPlus.Services.UserProfile;

public interface IUserProfileService
{
    Task<UserProfileDto?> GetProfileAsync(long userId, CancellationToken cancellationToken = default);

    Task<UpdateProfileResult> UpdateProfileAsync(
        long userId,
        UpdateProfileRequest request,
        CancellationToken cancellationToken = default);
}