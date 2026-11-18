using CsuPlusPlus.Dtos.File;

namespace CsuPlusPlus.Services.File;

public record CreateFileResponse(
    CreateFileStatus Status,
    FileMetadataResponse? Response
);

public enum CreateFileStatus
{
    Success,
    TooLarge,
    FormatNotSupported,
    CourseNotFound,
    CourseInOtherUniversity,
    Duplicate,
    DailyUploadLimitExceeded,
    UserNotFound
}