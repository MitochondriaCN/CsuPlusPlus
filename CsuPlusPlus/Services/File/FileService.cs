using CsuPlusPlus.Dtos.File;
using CsuPlusPlus.Infra;
using Microsoft.EntityFrameworkCore;

namespace CsuPlusPlus.Services.File;

public class FileService(AppDbContext db) : IFileService
{
    public async Task<FileMetadataResponse?> GetFileMetadata(long id, CancellationToken cancellationToken)
    {
        var file = await db.Files.AsNoTracking()
            .Include(f => f.Updater)
            .Include(f => f.Course)
            .SingleOrDefaultAsync(f => f.Id == id, cancellationToken);

        return file == null
            ? null
            : new FileMetadataResponse(
                file.Id,
                file.GuidFilename.ToString(),
                Convert.ToHexStringLower(file.Sha256),
                file.Updater.Id,
                file.Updater.Nickname.Value,
                file.Course?.Id,
                file.Course?.Name.Value);
    }
}