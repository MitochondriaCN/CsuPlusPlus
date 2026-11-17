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
            return new GetFileMetadataResponse(GetFileMetadataResult.NotFound);

        if (universityId != null
            && !await db.CourseFiles.AsNoTracking()
                .AnyAsync(cf => cf.FileId == id && cf.Course.University.Id == universityId, cancellationToken))
            return new GetFileMetadataResponse(GetFileMetadataResult.Forbidden);

        return new GetFileMetadataResponse(GetFileMetadataResult.Success, new FileMetadataResponse(
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
            return new CreateFileResponse(CreateFileResult.CourseNotFound, null);
        if (course.University.Id != universityId)
            return new CreateFileResponse(CreateFileResult.CourseInOtherUniversity, null);

        return await CreateFileAsync(request, uploaderUserId, course, cancellationToken);
    }

    public async Task<DownloadFileResponse> DownloadFileAsync(long id, CancellationToken cancellationToken)
    {
        var file = await db.Files.AsNoTracking()
            .SingleOrDefaultAsync(f => f.Id == id, cancellationToken);
        if (file == null)
            return new DownloadFileResponse(DownloadFileResult.NotFound);

        return OpenFile(file);
    }

    public async Task<DownloadFileResponse> DownloadCourseFileAsync(long id, long universityId,
        CancellationToken cancellationToken)
    {
        var file = await db.Files.AsNoTracking()
            .SingleOrDefaultAsync(f => f.Id == id, cancellationToken);
        if (file == null)
            return new DownloadFileResponse(DownloadFileResult.NotFound);

        var sameUniversity = await db.CourseFiles.AsNoTracking()
            .AnyAsync(cf => cf.FileId == id && cf.Course.University.Id == universityId, cancellationToken);
        if (!sameUniversity)
            return new DownloadFileResponse(DownloadFileResult.Forbidden);

        return OpenFile(file);
    }

    public async Task<BindCourseFileResponse> BindCourseFileAsync(long courseId, long fileId, long universityId,
        CancellationToken cancellationToken)
    {
        var course = await db.Courses
            .Include(course => course.University)
            .SingleOrDefaultAsync(c => c.Id == courseId, cancellationToken);
        if (course == null)
            return new BindCourseFileResponse(BindCourseFileResult.CourseNotFound);
        if (course.University.Id != universityId)
            return new BindCourseFileResponse(BindCourseFileResult.CourseInOtherUniversity);

        var file = await db.Files
            .SingleOrDefaultAsync(f => f.Id == fileId, cancellationToken);
        if (file == null)
            return new BindCourseFileResponse(BindCourseFileResult.FileNotFound);

        if (await db.CourseFiles.AnyAsync(cf => cf.CourseId == courseId && cf.FileId == fileId,
                cancellationToken))
            return new BindCourseFileResponse(BindCourseFileResult.Duplicate);

        db.CourseFiles.Add(new CourseFile { Course = course, File = file });
        try
        {
            await db.SaveChangesAsync(cancellationToken);
        }
        catch (DbUpdateException)
        {
            return new BindCourseFileResponse(BindCourseFileResult.Duplicate);
        }

        return new BindCourseFileResponse(BindCourseFileResult.Success,
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

    private async Task<CreateFileResponse> CreateFileAsync(CreateFileRequest request, long uploaderUserId,
        Models.Course? course, CancellationToken cancellationToken)
    {
        var user = await db.Users
            .Where(u => u.Id == uploaderUserId)
            .SingleOrDefaultAsync(cancellationToken);
        if (user == null)
            return new CreateFileResponse(CreateFileResult.UserNotFound, null);

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

        // 哈希
        byte[] sha256;
        await using (var stream = request.File.OpenReadStream())
            sha256 = await SHA256.HashDataAsync(stream, cancellationToken);

        // 查重
        var existingFile = await db.Files.AsNoTracking()
            .SingleOrDefaultAsync(f => f.Sha256 == sha256, cancellationToken);
        if (existingFile != null)
            return new CreateFileResponse(CreateFileResult.Duplicate,
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
            return new CreateFileResponse(CreateFileResult.Duplicate, null);
        }

        return new CreateFileResponse(CreateFileResult.Success,
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