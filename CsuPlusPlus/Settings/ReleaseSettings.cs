namespace CsuPlusPlus.Settings;

public record ReleaseSettings(
    uint CurrentBuildCode = 1,
    string CurrentBuildName = "0.1.0",
    string ReleaseUrl = "https://github.com/MitochondriaCN/CsuPlusPlus"
)
{
    public static readonly ReleaseSettings Default = new();
}