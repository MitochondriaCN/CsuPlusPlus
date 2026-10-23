namespace CsuPlusPlus.Dtos.Auth;

public record LoginResponse(string Token, DateTimeOffset ExpiresAt, UserProfileDto User);