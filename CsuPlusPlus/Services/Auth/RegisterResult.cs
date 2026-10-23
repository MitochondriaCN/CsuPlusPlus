using CsuPlusPlus.Dtos.Auth;

namespace CsuPlusPlus.Services.Auth;

public enum RegisterStatus
{
    Success,
    EmailAlreadyExists,
    CollegeNotFound,
    InvalidProfile
}

public record RegisterResult(RegisterStatus Status, LoginResponse? Response = null);