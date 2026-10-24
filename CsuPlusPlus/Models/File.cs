using System.ComponentModel.DataAnnotations.Schema;

namespace CsuPlusPlus.Models;

[Table("files")]
public class File
{
    public long Id { get; set; }

    public required Guid GuidFilename { get; set; }

    public required User Updater { get; set; }

    public Course? Course { get; set; }
}