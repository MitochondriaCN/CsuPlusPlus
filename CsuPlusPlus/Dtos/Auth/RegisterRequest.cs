using System.ComponentModel.DataAnnotations;

namespace CsuPlusPlus.Dtos.Auth;

public record RegisterRequest
{
    [Required] [EmailAddress] public string Email { get; init; } = string.Empty;

    [Required]
    [MinLength(8)]
    [MaxLength(128)]
    public string Password { get; init; } = string.Empty;

    [Required] [MaxLength(20)] public string Nickname { get; init; } = string.Empty;

    [Range(1, long.MaxValue)] public long CollegeId { get; init; }
}