using System.ComponentModel.DataAnnotations;

namespace CsuPlusPlus.Dtos.Course;

public class CreateCourseRequest
{
    [Required]
    [StringLength(50, MinimumLength = 1, ErrorMessage = "课程名称长度必须为 1 - 50 字符。")]
    public required string Name { get; init; }

    public long? CollegeId { get; init; }

    [StringLength(20, MinimumLength = 1, ErrorMessage = "课程别名长度必须为 1 - 20 字符。")]
    public string? Alias { get; init; }

    [Range(1, double.MaxValue, ErrorMessage = "课程学时数必须大于或等于 1。")]
    public uint? Hours { get; init; }
}