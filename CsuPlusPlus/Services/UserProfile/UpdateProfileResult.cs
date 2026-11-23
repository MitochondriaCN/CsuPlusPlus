using CsuPlusPlus.Dtos.Auth;

namespace CsuPlusPlus.Services.UserProfile;

public record UpdateProfileResult(
    UpdateProfileStatus Status,
    UserProfileDto? Response = null);

public enum UpdateProfileStatus
{
    Success,
    UserNotFound,
    InvalidProfile
}