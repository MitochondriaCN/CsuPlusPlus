namespace CsuPlusPlus.Settings;

public record UserFileSettings(
    uint MaxUploadsOfDay = 10,
    long MaxFileSizeBytes = 1024 * 1024 * 10L // 10 MB
)
{
    public static readonly UserFileSettings Default = new();
}