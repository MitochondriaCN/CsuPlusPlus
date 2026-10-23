using CsuPlusPlus.Dtos.University;

namespace CsuPlusPlus.Services.University;

public interface IUniversityService
{
    Task<CreateUniversityResult> CreateUniversityAsync(
        CreateUniversityRequest request, CancellationToken cancellationToken);
}