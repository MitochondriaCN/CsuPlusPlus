namespace CsuPlusPlus.Dtos.Release;

public record ReleaseResponse(
    uint CurrentBuildCode,
    string CurrentBuildName,
    string ReleaseUrl
);