namespace CsuPlusPlus.Authorization;

public interface ICurrentUser
{
    long? UserId { get; }

    long? CollegeId { get; }

    long? UniversityId { get; }
}