namespace CsuPlusPlus.Dtos.Auth;

public record UserProfileDto(
    long Id,
    string Email,
    string Nickname,
    long UniversityId,
    string UniversityName,
    long CollegeId,
    string CollegeName
);