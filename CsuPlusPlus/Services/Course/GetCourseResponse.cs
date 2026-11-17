using CsuPlusPlus.Dtos.Common;
using CsuPlusPlus.Dtos.Course;

namespace CsuPlusPlus.Services.Course;

public record GetSingleCourseResponse(
    GetCourseResult Result,
    CourseResponse? Response = null
);

public record GetCourseResponse(
    GetCourseResult Result,
    PagedResponse<CourseResponse> Response
);

public enum GetCourseResult
{
    Success,
    UniversityNotFound,
    CollegeNotFound,
    CourseNotFound
}