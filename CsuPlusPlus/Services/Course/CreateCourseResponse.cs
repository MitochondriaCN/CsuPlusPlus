using CsuPlusPlus.Dtos.Course;

namespace CsuPlusPlus.Services.Course;

public record CreateCourseResponse(
    CreateCourseStatus Status,
    CourseResponse? Response = null
);

public enum CreateCourseStatus
{
    Success,
    UniversityNotFound,
    CollegeNotFound,
    CollegeInOtherUniversity
}