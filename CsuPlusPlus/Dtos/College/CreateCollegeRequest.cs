using System.ComponentModel.DataAnnotations;
using CsuPlusPlus.Models.ValueObjects;

namespace CsuPlusPlus.Dtos.College;

public record CreateCollegeRequest
{
    [Range(1, long.MaxValue, ErrorMessage = "大学 ID 必须为正数。")]
    public required long UniversityId { get; init; }

    [Required]
    [StringLength(
        NonEmptyName.MaxLength,
        MinimumLength = 2,
        ErrorMessage = "大学名称长度必须在{2}到{1}个字符之间。")]
    public required string Name { get; init; }
}