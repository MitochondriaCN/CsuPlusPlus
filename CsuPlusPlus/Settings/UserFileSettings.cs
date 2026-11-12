namespace CsuPlusPlus.Settings;

public record UserFileSettings(
    uint MaxUploadsOfDay = 10
)
{
    public static readonly UserFileSettings Default = new();
}