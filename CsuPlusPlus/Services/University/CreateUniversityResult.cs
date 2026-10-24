using CsuPlusPlus.Dtos.University;

namespace CsuPlusPlus.Services.University;

public record CreateUniversityResult(
    CreateUniversityStatus Status,
    UniversityResponse? Response = null);

public enum CreateUniversityStatus
{
    Success,
    NameAlreadyExists
}