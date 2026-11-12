namespace CsuPlusPlus.Dtos.File;

public record FileMetadataResponse(
    long Id,
    string GuidFilename,
    string Sha256Hash,
    long UpdaterUserId,
    string UpdaterUserNickname,
    long? CourseId,
    string? CourseName
);