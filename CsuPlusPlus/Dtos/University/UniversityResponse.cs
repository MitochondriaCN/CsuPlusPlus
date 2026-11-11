namespace CsuPlusPlus.Dtos.University;

public record UniversityResponse(
    long Id,
    string Name,
    string? EmblemFilename
);