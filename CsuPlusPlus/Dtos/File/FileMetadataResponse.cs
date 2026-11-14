namespace CsuPlusPlus.Dtos.File;

public record FileMetadataResponse(
    long Id,
    string GuidFilename,
    string? FriendlyFilename,
    string Extension,
    string Sha256Hash,
    long UploaderUserId,
    string UploaderUserNickname,
    long? CourseId,
    string? CourseName
);