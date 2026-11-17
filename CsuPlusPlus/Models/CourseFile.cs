using System.ComponentModel.DataAnnotations.Schema;
using Microsoft.EntityFrameworkCore;

namespace CsuPlusPlus.Models;

[Table("course_files")]
[PrimaryKey(nameof(CourseId), nameof(FileId))]
public class CourseFile
{
    public long CourseId { get; set; }

    public required Course Course { get; set; }

    public long FileId { get; set; }

    public required File File { get; set; }
}