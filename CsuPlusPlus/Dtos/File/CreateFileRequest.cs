using System.ComponentModel.DataAnnotations;

namespace CsuPlusPlus.Dtos.File;

public record CreateFileRequest
{
    [Required] public required IFormFile File { get; init; }

    [MaxLength(30, ErrorMessage = "文件名不能超过 30 个字符。")]
    public string? FriendlyFilename { get; init; }
}