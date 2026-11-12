using System.ComponentModel.DataAnnotations.Schema;
using Microsoft.EntityFrameworkCore;

namespace CsuPlusPlus.Models;

[Table("files")]
[Index(nameof(Sha256), IsUnique = true)]
public class File
{
    public long Id { get; set; }

    public required Guid GuidFilename { get; set; }

    public required byte[] Sha256 { get; set; }

    public required User Updater { get; set; }

    public Course? Course { get; set; }

    public required DateTimeOffset CreatedAt { get; set; }
}