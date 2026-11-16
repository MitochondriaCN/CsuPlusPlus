using CsuPlusPlus.Dtos.Common;
using CsuPlusPlus.Dtos.Course;
using CsuPlusPlus.Dtos.File;
using CsuPlusPlus.Infra;
using CsuPlusPlus.Models;
using Microsoft.EntityFrameworkCore;

namespace CsuPlusPlus.Services.File;

public class CourseFileService(AppDbContext db) : ICourseFileService
{
    public async Task<BindCourseFileResponse> BindCourseFile(long courseId, long fileId,
        CancellationToken cancellationToken)
    {
        var file = await db.Files.SingleOrDefaultAsync(f => f.Id == fileId, cancellationToken);
        if (file == null)
            return new BindCourseFileResponse(BindCourseFileResult.FileNotFound);

        var course = await db.Courses.SingleOrDefaultAsync(c => c.Id == courseId, cancellationToken);
        if (course == null)
            return new BindCourseFileResponse(BindCourseFileResult.CourseNotFound);

        if (await db.CourseFiles.AnyAsync(
                cf => cf.CourseId == courseId && cf.FileId == fileId, cancellationToken))
            return new BindCourseFileResponse(BindCourseFileResult.Duplicate);

        db.CourseFiles.Add(new CourseFile
        {
            Course = course,
            File = file
        });

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

    public async Task<PagedResponse<FileMetadataResponse>> GetFilesBy(long courseId, PageRequest pageRequest,
        CancellationToken cancellationToken)
    {
        var query = db.CourseFiles.AsNoTracking()
            .Where(cf => cf.CourseId == courseId)
            .OrderBy(cf => cf.FileId);
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
}