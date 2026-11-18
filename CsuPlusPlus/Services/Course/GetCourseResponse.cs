using CsuPlusPlus.Dtos.Common;
using CsuPlusPlus.Dtos.Course;

namespace CsuPlusPlus.Services.Course;

public record GetSingleCourseResponse(
    GetCourseStatus Status,
    CourseResponse? Response = null
);

public record GetCourseResponse(
    GetCourseStatus Status,
    PagedResponse<CourseResponse>? Response = null
);

public enum GetCourseStatus
{
    Success,
    UniversityNotFound,
    CollegeNotFound,
    CollegeInOtherUniversity,
    CourseNotFound,
    CourseInOtherUniversity
}