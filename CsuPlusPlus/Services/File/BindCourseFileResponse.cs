using CsuPlusPlus.Dtos.File;

namespace CsuPlusPlus.Services.File;

public record BindCourseFileResponse(
    BindCourseFileStatus Status,
    CourseFileResponse? Response = null);

public enum BindCourseFileStatus
{
    Success,
    FileNotFound,
    CourseNotFound,
    CourseInOtherUniversity,
    Duplicate
}