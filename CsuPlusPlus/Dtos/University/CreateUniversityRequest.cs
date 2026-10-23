using System.ComponentModel.DataAnnotations;
using CsuPlusPlus.Models.ValueObjects;

namespace CsuPlusPlus.Dtos.University;

public record CreateUniversityRequest
{
    [Required]
    [StringLength(
        NonEmptyName.MaxLength,
        MinimumLength = 2,
        ErrorMessage = "大学名称长度必须在{2}到{1}个字符之间。")]
    public required string Name { get; init; }
}