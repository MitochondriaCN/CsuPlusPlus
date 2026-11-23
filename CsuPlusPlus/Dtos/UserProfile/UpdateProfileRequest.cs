using System.ComponentModel.DataAnnotations;

namespace CsuPlusPlus.Dtos.UserProfile;

public record UpdateProfileRequest
{
    [Required(ErrorMessage = "昵称不能为空。")]
    [StringLength(20, MinimumLength = 1, ErrorMessage = "昵称长度必须在{2}到{1}个字符之间。")]
    public required string Nickname { get; init; }
}