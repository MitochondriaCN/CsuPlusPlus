using System.Security.Cryptography;
using CsuPlusPlus.Dtos.Common;
using CsuPlusPlus.Dtos.Course;
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
    public async Task<GetFileMetadataResponse> GetFileMetadataAsync(long id, CancellationToken cancellationToken,
        long? universityId = null)
    {
        var file = await db.Files.AsNoTracking()
            .Include(f => f.Uploader)
            .SingleOrDefaultAsync(f => f.Id == id, cancellationToken);
        if (file == null)
            return new GetFileMetadataResponse(GetFileMetadataStatus.NotFound);

        if (universityId != null
            && !await db.CourseFiles.AsNoTracking()
                .AnyAsync(cf => cf.FileId == id && cf.Course.University.Id == universityId, cancellationToken))
            return new GetFileMetadataResponse(GetFileMetadataStatus.Forbidden);

        return new GetFileMetadataResponse(GetFileMetadataStatus.Success, new FileMetadataResponse(
            Id: file.Id,
            GuidFilename: file.GuidFilename.ToString("N"),
            FriendlyFilename: file.FriendlyFilename,
            Extension: file.Extension,
            Sha256Hash: Convert.ToHexStringLower(file.Sha256),
            UploaderUserId: file.Uploader.Id,
            UploaderUserNickname: file.Uploader.Nickname.Value));
    }

    public Task<CreateFileResponse> CreateFileAsync(CreateFileRequest request, long uploaderUserId,
        CancellationToken cancellationToken) =>
        CreateFileAsync(request, uploaderUserId, null, cancellationToken);

    public async Task<CreateFileResponse> CreateCourseFileAsync(CreateFileRequest request, long uploaderUserId,
        long courseId, long universityId, CancellationToken cancellationToken)
    {
        var course = await db.Courses.Include(c => c.University)
            .SingleOrDefaultAsync(c => c.Id == courseId, cancellationToken);
        if (course == null)
            return new CreateFileResponse(CreateFileStatus.CourseNotFound, null);
        if (course.University.Id != universityId)
            return new CreateFileResponse(CreateFileStatus.CourseInOtherUniversity, null);

        return await CreateFileAsync(request, uploaderUserId, course, cancellationToken);
    }

    public async Task<DownloadFileResponse> DownloadFileAsync(long id, CancellationToken cancellationToken)
    {
        var file = await db.Files.AsNoTracking()
            .SingleOrDefaultAsync(f => f.Id == id, cancellationToken);
        if (file == null)
            return new DownloadFileResponse(DownloadFileStatus.NotFound);

        return OpenFile(file);
    }

    public async Task<DownloadFileResponse> DownloadCourseFileAsync(long id, long universityId,
        CancellationToken cancellationToken)
    {
        var file = await db.Files.AsNoTracking()
            .SingleOrDefaultAsync(f => f.Id == id, cancellationToken);
        if (file == null)
            return new DownloadFileResponse(DownloadFileStatus.NotFound);

        var sameUniversity = await db.CourseFiles.AsNoTracking()
            .AnyAsync(cf => cf.FileId == id && cf.Course.University.Id == universityId, cancellationToken);
        if (!sameUniversity)
            return new DownloadFileResponse(DownloadFileStatus.Forbidden);

        return OpenFile(file);
    }

    public async Task<BindCourseFileResponse> BindCourseFileAsync(long courseId, long fileId, long universityId,
        CancellationToken cancellationToken)
    {
        var course = await db.Courses
            .Include(course => course.University)
            .SingleOrDefaultAsync(c => c.Id == courseId, cancellationToken);
        if (course == null)
            return new BindCourseFileResponse(BindCourseFileStatus.CourseNotFound);
        if (course.University.Id != universityId)
            return new BindCourseFileResponse(BindCourseFileStatus.CourseInOtherUniversity);

        var file = await db.Files
            .SingleOrDefaultAsync(f => f.Id == fileId, cancellationToken);
        if (file == null)
            return new BindCourseFileResponse(BindCourseFileStatus.FileNotFound);

        if (await db.CourseFiles.AnyAsync(cf => cf.CourseId == courseId && cf.FileId == fileId,
                cancellationToken))
            return new BindCourseFileResponse(BindCourseFileStatus.Duplicate);

        db.CourseFiles.Add(new CourseFile { Course = course, File = file });
        try
        {
            await db.SaveChangesAsync(cancellationToken);
        }
        catch (DbUpdateException)
        {
            return new BindCourseFileResponse(BindCourseFileStatus.Duplicate);
        }

        return new BindCourseFileResponse(BindCourseFileStatus.Success,
            new CourseFileResponse(courseId, fileId));
    }

    public Task<PagedResponse<FileMetadataResponse>> GetFilesBy(long universityId, long courseId,
        PageRequest pageRequest, CancellationToken cancellationToken) =>
        QueryFilesAsync(universityId, courseId, null, pageRequest, cancellationToken);

    public Task<PagedResponse<FileMetadataResponse>> SearchFilesByAsync(long universityId, long courseId,
        string keyword, PageRequest pageRequest, CancellationToken cancellationToken) =>
        QueryFilesAsync(universityId, courseId, keyword, pageRequest, cancellationToken);

    public async Task<PagedResponse<CourseResponse>> GetCoursesBy(long fileId, PageRequest pageRequest,
        CancellationToken cancellationToken)
    {
        var query = db.CourseFiles.AsNoTracking()
            .Where(cf => cf.FileId == fileId)
            .OrderBy(cf => cf.CourseId);
        var totalCount = await query.LongCountAsync(cancellationToken);
        var items = await query
            .Skip(pageRequest.Skip)
            .Take(pageRequest.PageSize)
            .Select(cf => new CourseResponse(
                cf.Course.Id,
                cf.Course.Name.Value,
                cf.Course.University.Id,
                cf.Course.University.Name.Value,
                cf.Course.College == null ? null : cf.Course.College.Id,
                cf.Course.College == null ? null : cf.Course.College.Name.Value,
                cf.Course.Alias == null ? null : cf.Course.Alias.Value,
                cf.Course.Hours,
                cf.Course.CreatedAt,
                cf.Course.UpdatedAt))
            .ToListAsync(cancellationToken);

        return new PagedResponse<CourseResponse>(
            Items: items,
            Page: pageRequest.Page,
            PageSize: pageRequest.PageSize,
            TotalCount: totalCount);
    }

    public async Task<DeleteFileResponse> DeleteFileAsync(long id, CancellationToken cancellationToken,
        long? uploaderUserId = null)
    {
        var file = await db.Files
            .Include(f => f.Uploader)
            .SingleOrDefaultAsync(f => f.Id == id, cancellationToken);
        if (file == null)
            return new DeleteFileResponse(DeleteFileStatus.NotFound);

        if (uploaderUserId is { } requiredUploader && file.Uploader.Id != requiredUploader)
            return new DeleteFileResponse(DeleteFileStatus.Forbidden);

        var universitiesWithEmblem = await db.Universities
            .Where(u => u.Emblem != null && u.Emblem.Id == id)
            .ToListAsync(cancellationToken);
        foreach (var u in universitiesWithEmblem)
            u.Emblem = null;

        var courseFiles = await db.CourseFiles
            .Where(cf => cf.FileId == id)
            .ToListAsync(cancellationToken);
        db.CourseFiles.RemoveRange(courseFiles);

        db.Files.Remove(file);
        await db.SaveChangesAsync(cancellationToken);

        var path = Path.Combine(fileStorageOptions.Value.RootPath,
            file.GuidFilename.ToString("N") + file.Extension);
        if (System.IO.File.Exists(path))
        {
            try
            {
                System.IO.File.Delete(path);
            }
            catch (Exception)
            {
            }
        }

        return new DeleteFileResponse(DeleteFileStatus.Success);
    }

    private async Task<CreateFileResponse> CreateFileAsync(CreateFileRequest request, long uploaderUserId,
        Models.Course? course, CancellationToken cancellationToken)
    {
        var user = await db.Users
            .Where(u => u.Id == uploaderUserId)
            .SingleOrDefaultAsync(cancellationToken);
        if (user == null)
            return new CreateFileResponse(CreateFileStatus.UserNotFound, null);

        var settings =
            await settingsService.GetAsync(SettingsKey.UserFile, UserFileSettings.Default, cancellationToken);

        // 查用户今天上传多少文件
        var startOfToday = new DateTimeOffset(DateTime.UtcNow.Date, TimeSpan.Zero);
        var todayUploadCount = await db.Files.AsNoTracking()
            .Where(f => f.Uploader.Id == uploaderUserId
                        && f.CreatedAt >= startOfToday)
            .CountAsync(cancellationToken);
        var maxUploadCount = settings.MaxUploadsOfDay;

        if (todayUploadCount >= maxUploadCount)
            return new CreateFileResponse(CreateFileStatus.DailyUploadLimitExceeded, null);

        // 校验大小
        if (request.File.Length > settings.MaxFileSizeBytes)
            return new CreateFileResponse(CreateFileStatus.TooLarge, null);

        // 校验格式
        FileFormat format;
        await using (var stream = request.File.OpenReadStream())
            format = await FileFormatSniffer.DetectAsync(stream, cancellationToken, request.File.FileName);

        var extension = format.ToExtension();
        if (extension is null)
            return new CreateFileResponse(CreateFileStatus.FormatNotSupported, null);

        // 哈希
        byte[] sha256;
        await using (var stream = request.File.OpenReadStream())
            sha256 = await SHA256.HashDataAsync(stream, cancellationToken);

        // 查重
        var existingFile = await db.Files.AsNoTracking()
            .SingleOrDefaultAsync(f => f.Sha256 == sha256, cancellationToken);
        if (existingFile != null)
            return new CreateFileResponse(CreateFileStatus.Duplicate,
                (await GetFileMetadataAsync(existingFile.Id, cancellationToken)).Response);

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
            CreatedAt = DateTimeOffset.UtcNow,
            Extension = extension
        };
        db.Files.Add(file);
        if (course != null)
            db.CourseFiles.Add(new CourseFile { Course = course, File = file });

        try
        {
            await db.SaveChangesAsync(cancellationToken);
        }
        catch (DbUpdateException)
        {
            System.IO.File.Delete(path);
            return new CreateFileResponse(CreateFileStatus.Duplicate, null);
        }

        return new CreateFileResponse(CreateFileStatus.Success,
            (await GetFileMetadataAsync(file.Id, cancellationToken)).Response);
    }

    private async Task<PagedResponse<FileMetadataResponse>> QueryFilesAsync(long universityId, long courseId,
        string? keyword, PageRequest pageRequest, CancellationToken cancellationToken)
    {
        var query = db.CourseFiles.AsNoTracking()
            .Where(cf => cf.CourseId == courseId && cf.Course.University.Id == universityId);

        if (!string.IsNullOrWhiteSpace(keyword))
        {
            var pattern = $"%{keyword.Trim()}%";
            query = query.Where(cf => EF.Functions.Like(cf.File.FriendlyFilename, pattern));
        }

        query = query.OrderBy(cf => cf.FileId);

        var totalCount = await query.LongCountAsync(cancellationToken);
        var items = await query
            .Skip(pageRequest.Skip)
            .Take(pageRequest.PageSize)
            .Select(cf => new
            {
                cf.File.Id,
                cf.File.GuidFilename,
                cf.File.FriendlyFilename,
                cf.File.Extension,
                cf.File.Sha256,
                UploaderUserId = cf.File.Uploader.Id,
                UploaderUserNickname = cf.File.Uploader.Nickname.Value
            })
            .ToListAsync(cancellationToken);

        return new PagedResponse<FileMetadataResponse>(
            Items: items.Select(f => new FileMetadataResponse(
                Id: f.Id,
                GuidFilename: f.GuidFilename.ToString("N"),
                FriendlyFilename: f.FriendlyFilename,
                Extension: f.Extension,
                Sha256Hash: Convert.ToHexStringLower(f.Sha256),
                UploaderUserId: f.UploaderUserId,
                UploaderUserNickname: f.UploaderUserNickname)).ToList(),
            Page: pageRequest.Page,
            PageSize: pageRequest.PageSize,
            TotalCount: totalCount);
    }

    private DownloadFileResponse OpenFile(Models.File file)
    {
        var path = Path.Combine(fileStorageOptions.Value.RootPath,
            file.GuidFilename.ToString("N") + file.Extension);
        if (!System.IO.File.Exists(path))
            return new DownloadFileResponse(DownloadFileStatus.ContentMissing);

        var stream = new FileStream(path, FileMode.Open, FileAccess.Read, FileShare.Read,
            64 * 1024, useAsync: true);
        var downloadName = string.IsNullOrWhiteSpace(file.FriendlyFilename)
            ? file.GuidFilename.ToString("N")
            : Path.GetFileName(file.FriendlyFilename);

        return new DownloadFileResponse(
            DownloadFileStatus.Success,
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
            ".md" => "text/markdown; charset=utf-8",
            ".png" => "image/png",
            ".jpg" => "image/jpeg",
            ".gif" => "image/gif",
            ".webp" => "image/webp",
            _ => "application/octet-stream"
        };
}