namespace CsuPlusPlus.Options;

public class FileStorageOptions
{
    public const string SectionName = "FileStorage";

    public string RootPath { get; init; } = "storage/files";
}