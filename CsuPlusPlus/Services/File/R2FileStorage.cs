using System.Net;
using Amazon.Runtime;
using Amazon.S3;
using Amazon.S3.Model;
using CsuPlusPlus.Dtos.File;
using CsuPlusPlus.Options;
using Microsoft.Extensions.Options;

namespace CsuPlusPlus.Services.File;

public sealed class R2FileStorage : IFileStorage, IDisposable
{
    private readonly R2StorageOptions _options;
    private readonly AmazonS3Client _s3Client;

    public R2FileStorage(IOptions<R2StorageOptions> options)
    {
        _options = options.Value;
        if (string.IsNullOrWhiteSpace(_options.ServiceUrl)
            || string.IsNullOrWhiteSpace(_options.BucketName)
            || string.IsNullOrWhiteSpace(_options.AccessKeyId)
            || string.IsNullOrWhiteSpace(_options.SecretAccessKey)
            || _options.PresignExpiryMinutes <= 0)
        {
            throw new InvalidOperationException(
                "R2Storage 配置无效：需提供 ServiceUrl、BucketName、AccessKeyId、SecretAccessKey 和正数 PresignExpiryMinutes。");
        }

        var credentials = new BasicAWSCredentials(_options.AccessKeyId, _options.SecretAccessKey);
        var config = new AmazonS3Config
        {
            ServiceURL = _options.ServiceUrl,
            ForcePathStyle = true,
            RequestChecksumCalculation = RequestChecksumCalculation.WHEN_REQUIRED,
            ResponseChecksumValidation = ResponseChecksumValidation.WHEN_REQUIRED
        };

        _s3Client = new AmazonS3Client(credentials, config);
    }

    public void Dispose() => _s3Client.Dispose();

    public async Task UploadAsync(string objectKey, Stream content, string contentType,
        CancellationToken cancellationToken)
    {
        var request = new PutObjectRequest
        {
            BucketName = _options.BucketName,
            Key = objectKey,
            InputStream = content,
            ContentType = contentType,
            AutoCloseStream = false,
            DisablePayloadSigning = true
        };

        await _s3Client.PutObjectAsync(request, cancellationToken);
    }

    public async Task<bool> ExistsAsync(string objectKey, CancellationToken cancellationToken)
    {
        try
        {
            await _s3Client.GetObjectMetadataAsync(_options.BucketName, objectKey, cancellationToken);
            return true;
        }
        catch (AmazonS3Exception ex) when (ex.StatusCode == HttpStatusCode.NotFound)
        {
            return false;
        }
    }

    public Task DeleteAsync(string objectKey, CancellationToken cancellationToken) =>
        _s3Client.DeleteObjectAsync(_options.BucketName, objectKey, cancellationToken);

    public DownloadFileUrlResponse CreatePresignedDownloadUrl(string objectKey, string downloadName, string contentType)
    {
        var expiresAt = DateTimeOffset.UtcNow.AddMinutes(_options.PresignExpiryMinutes);
        var encodedName = Uri.EscapeDataString(downloadName);
        var request = new GetPreSignedUrlRequest
        {
            BucketName = _options.BucketName,
            Key = objectKey,
            Verb = HttpVerb.GET,
            Expires = expiresAt.UtcDateTime,
            ResponseHeaderOverrides = new ResponseHeaderOverrides
            {
                ContentType = contentType,
                ContentDisposition = $"attachment; filename*=UTF-8''{encodedName}"
            }
        };

        var url = _s3Client.GetPreSignedURL(request);
        return new DownloadFileUrlResponse(url, expiresAt, downloadName, contentType);
    }
}