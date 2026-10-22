using System.ComponentModel.DataAnnotations.Schema;
using CsuPlusPlus.Models.ValueObjects;

namespace CsuPlusPlus.Models;

[Table("courses")]
public class Course
{
    public long Id { get; set; }

    public required NonEmptyName Name { get; set; }

    public required University University { get; set; }

    public College? College { get; set; }

    public NonEmptyName? Alias { get; set; }

    public uint? Hours { get; set; }

    public DateTimeOffset CreatedAt { get; set; }

    public DateTimeOffset UpdatedAt { get; set; }
}