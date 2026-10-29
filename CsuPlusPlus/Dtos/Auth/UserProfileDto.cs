namespace CsuPlusPlus.Dtos.Auth;

public record UserProfileDto(long Id, string Email, string Nickname, long CollegeId, string CollegeName);