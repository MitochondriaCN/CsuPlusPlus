using CsuPlusPlus.Dtos.Common;
using CsuPlusPlus.Dtos.University;

namespace CsuPlusPlus.Services.University;

public interface IUniversityService
{
    Task<CreateUniversityResult> CreateUniversityAsync(
        CreateUniversityRequest request, CancellationToken cancellationToken);

    Task<PagedResponse<UniversityResponse>> ListUniversitiesAsync
        (PageRequest request, CancellationToken cancellationToken);
}