namespace CsuPlusPlus.Options;

public class R2StorageOptions
{
    public const string SectionName = "R2Storage";

    public string ServiceUrl { get; init; } = string.Empty;
    public string BucketName { get; init; } = string.Empty;
    public string AccessKeyId { get; init; } = string.Empty;
    public string SecretAccessKey { get; init; } = string.Empty;
    public int PresignExpiryMinutes { get; init; } = 15;
}
