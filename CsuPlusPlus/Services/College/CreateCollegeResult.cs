using CsuPlusPlus.Dtos.College;

namespace CsuPlusPlus.Services.College;

public record CreateCollegeResult(
    CreateCollegeStatus Status,
    CollegeResponse? Response = null);

public enum CreateCollegeStatus
{
    Success,
    UniversityNotFound,
    NameConflictInUniversity
}