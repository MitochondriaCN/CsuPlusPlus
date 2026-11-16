namespace CsuPlusPlus.Dtos.Course;

public record CourseResponse(
    long Id,
    string Name,
    long UniversityId,
    string UniversityName,
    long? CollegeId,
    string? CollegeName,
    string? Alias,
    uint? Hours,
    DateTimeOffset CreatedAt,
    DateTimeOffset UpdatedAt
);