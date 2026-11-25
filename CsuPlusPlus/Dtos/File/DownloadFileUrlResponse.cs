namespace CsuPlusPlus.Dtos.File;

public record DownloadFileUrlResponse(
    string Url,
    DateTimeOffset ExpiresAt,
    string DownloadName,
    string ContentType
);
