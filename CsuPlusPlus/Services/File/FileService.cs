using System.Security.Cryptography;
using CsuPlusPlus.Dtos.File;
using CsuPlusPlus.Infra;
using CsuPlusPlus.Models;
using CsuPlusPlus.Options;
using CsuPlusPlus.Services.Setting;
using CsuPlusPlus.Settings;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Options;

namespace CsuPlusPlus.Services.File;

public class FileService(
    AppDbContext db,
    ISettingsService settingsService,
    IOptions<FileStorageOptions> fileStorageOptions) : IFileService
{
    public async Task<FileMetadataResponse?> GetFileMetadataAsync(long id, CancellationToken cancellationToken)
    {
        var file = await db.Files.AsNoTracking()
            .Include(f => f.Uploader)
            .Include(f => f.Course)
            .SingleOrDefaultAsync(f => f.Id == id, cancellationToken);

        return file == null
            ? null
            : new FileMetadataResponse(
                Id: file.Id,
                GuidFilename: file.GuidFilename.ToString("N"),
                FriendlyFilename: file.FriendlyFilename,
                Extension: file.Extension,
                Sha256Hash: Convert.ToHexStringLower(file.Sha256),
                UploaderUserId: file.Uploader.Id,
                UploaderUserNickname: file.Uploader.Nickname.Value,
                CourseId: file.Course?.Id,
                CourseName: file.Course?.Name.Value);
    }

    public async Task<CreateFileResponse> CreateFileAsync(CreateFileRequest request, long uploaderUserId,
        bool allowCrossUniversity, CancellationToken cancellationToken)
    {
        var user = await db.Users
            .Include(u => u.College)
            .ThenInclude(c => c.University)
            .Where(u => u.Id == uploaderUserId)
            .SingleOrDefaultAsync(cancellationToken);
        if (user == null)
            return new CreateFileResponse(CreateFileResult.UserNotFound, null);

        var settings =
            await settingsService.GetAsync(SettingsKey.UserFile, UserFileSettings.Default, cancellationToken);

        // 查用户今天上传多少文件
        var todayUploadCount = await db.Files.AsNoTracking()
            .Where(f => f.Uploader.Id == uploaderUserId
                        && f.CreatedAt >= DateTimeOffset.UtcNow.Date)
            .CountAsync(cancellationToken);
        var maxUploadCount = settings.MaxUploadsOfDay;

        if (todayUploadCount >= maxUploadCount)
            return new CreateFileResponse(CreateFileResult.DailyUploadLimitExceeded, null);

        // 校验大小
        if (request.File.Length > settings.MaxFileSizeBytes)
            return new CreateFileResponse(CreateFileResult.TooLarge, null);

        // 校验格式
        FileFormat format;
        await using (var stream = request.File.OpenReadStream())
            format = await FileFormatSniffer.DetectAsync(stream, cancellationToken);

        var extension = format.ToExtension();
        if (extension is null)
            return new CreateFileResponse(CreateFileResult.FormatNotSupported, null);

        // 查找目标课程
        Course? targetCourse = null;
        if (request.CourseId != null)
        {
            var course = await db.Courses
                .Include(c => c.University)
                .Where(c => c.Id == request.CourseId)
                .SingleOrDefaultAsync(cancellationToken);

            if (course == null)
                return new CreateFileResponse(CreateFileResult.CourseNotFound, null);

            if (!allowCrossUniversity && course.University.Id != user.College.University.Id)
                return new CreateFileResponse(CreateFileResult.CourseInOtherUniversity, null);

            targetCourse = course;
        }

        // 哈希
        byte[] sha256;
        await using (var stream = request.File.OpenReadStream())
            sha256 = await SHA256.HashDataAsync(stream, cancellationToken);

        // 查重
        var existingFile = await db.Files.AsNoTracking()
            .SingleOrDefaultAsync(f => f.Sha256 == sha256, cancellationToken);
        if (existingFile != null)
            return new CreateFileResponse(CreateFileResult.Duplicate,
                await GetFileMetadataAsync(existingFile.Id, cancellationToken));

        // 落盘
        var guidFileName = Guid.NewGuid();
        Directory.CreateDirectory(fileStorageOptions.Value.RootPath);
        var path = Path.Combine(fileStorageOptions.Value.RootPath, guidFileName.ToString("N") + extension);
        await using (var targetFile = new FileStream(path, FileMode.CreateNew, FileAccess.Write, FileShare.None))
        await using (var source = request.File.OpenReadStream())
            await source.CopyToAsync(targetFile, cancellationToken);

        // 入库
        var file = new Models.File
        {
            GuidFilename = guidFileName,
            FriendlyFilename = request.FriendlyFilename,
            Sha256 = sha256,
            Uploader = user,
            Course = targetCourse,
            CreatedAt = DateTimeOffset.UtcNow,
            Extension = extension
        };
        db.Files.Add(file);
        try
        {
            await db.SaveChangesAsync(cancellationToken);
        }
        catch (DbUpdateException)
        {
            System.IO.File.Delete(path);
            return new
                CreateFileResponse(CreateFileResult.Duplicate, null);
        }

        return new CreateFileResponse(CreateFileResult.Success,
            await GetFileMetadataAsync(file.Id, cancellationToken));
    }

    public async Task<DownloadFileResponse> DownloadFileAsync(long id, long userId,
        bool allowCrossUniversity, CancellationToken cancellationToken)
    {
        var file = await db.Files.AsNoTracking()
            .Include(f => f.Course)
            .ThenInclude(c => c!.University)
            .SingleOrDefaultAsync(f => f.Id == id, cancellationToken);
        if (file == null)
            return new DownloadFileResponse(DownloadFileResult.NotFound);

        if (!allowCrossUniversity && file.Course != null)
        {
            var userUniversityId = await db.Users.AsNoTracking()
                .Where(u => u.Id == userId)
                .Select(u => (long?)u.College.University.Id)
                .SingleOrDefaultAsync(cancellationToken);

            if (userUniversityId == null || userUniversityId != file.Course?.University.Id)
                return new DownloadFileResponse(DownloadFileResult.Forbidden);
        }

        var path = Path.Combine(fileStorageOptions.Value.RootPath,
            file.GuidFilename.ToString("N") + file.Extension);
        if (!System.IO.File.Exists(path))
            return new DownloadFileResponse(DownloadFileResult.ContentMissing);

        var stream = new FileStream(path, FileMode.Open, FileAccess.Read, FileShare.Read,
            64 * 1024, useAsync: true);
        var downloadName = string.IsNullOrWhiteSpace(file.FriendlyFilename)
            ? file.GuidFilename.ToString("N")
            : Path.GetFileName(file.FriendlyFilename);

        return new DownloadFileResponse(
            DownloadFileResult.Success,
            Content: stream,
            ContentType: GetContentType(file.Extension),
            DownloadName: downloadName + file.Extension);
    }

    private static string GetContentType(string extension) =>
        extension switch
        {
            ".pdf" => "application/pdf",
            ".docx" => "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            ".pptx" => "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            ".xlsx" => "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            ".zip" => "application/zip",
            _ => "application/octet-stream"
        };
}