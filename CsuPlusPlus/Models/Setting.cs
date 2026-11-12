using System.ComponentModel.DataAnnotations.Schema;
using Microsoft.EntityFrameworkCore;

namespace CsuPlusPlus.Models;

[Table("settings")]
[PrimaryKey(nameof(Key))]
public record Setting
{
    public required string Key { get; set; }

    /// <summary>
    /// JSON 配置值
    /// </summary>
    public required string Value { get; set; }

    public required DateTimeOffset UpdatedAt { get; set; }
}