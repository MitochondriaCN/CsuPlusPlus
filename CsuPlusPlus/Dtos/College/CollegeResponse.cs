namespace CsuPlusPlus.Dtos.College;

public record CollegeResponse(
    long Id,
    string Name,
    long UniversityId,
    string UniversityName);