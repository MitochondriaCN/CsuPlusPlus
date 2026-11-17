using CsuPlusPlus.Dtos.File;

namespace CsuPlusPlus.Services.File;

public record BindCourseFileResponse(
    BindCourseFileResult Result,
    CourseFileResponse? Response = null);

public enum BindCourseFileResult
{
    Success,
    FileNotFound,
    CourseNotFound,
    CourseInOtherUniversity,
    Duplicate
}