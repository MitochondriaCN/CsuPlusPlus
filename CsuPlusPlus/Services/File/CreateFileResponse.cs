using CsuPlusPlus.Dtos.File;

namespace CsuPlusPlus.Services.File;

public record CreateFileResponse(
    CreateFileResult Result,
    FileMetadataResponse? Response
);

public enum CreateFileResult
{
    Success,
    TooLarge,
    FormatNotSupported,
    CourseNotFound,
    Duplicate,
    DailyUploadLimitExceeded,
    UserNotFound
}