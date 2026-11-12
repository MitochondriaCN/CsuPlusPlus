using System.ComponentModel.DataAnnotations;

namespace CsuPlusPlus.Dtos.Common;

public record PageRequest
{
    [Required]
    [Range(1, int.MaxValue, ErrorMessage = "页码必须处于 1 到 int.MaxValue 之间。")]
    public required int Page { get; init; }

    [Range(1, 100, ErrorMessage = "每页项目数必须在 1 到 100 之间。")]
    public required int PageSize { get; init; }

    public int Skip => (Page - 1) * PageSize;
}