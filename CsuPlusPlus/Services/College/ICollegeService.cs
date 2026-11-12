using CsuPlusPlus.Dtos.College;
using CsuPlusPlus.Dtos.Common;

namespace CsuPlusPlus.Services.College;

public interface ICollegeService
{
    Task<CreateCollegeResult> CreateCollegeAsync(CreateCollegeRequest request, CancellationToken token);

    Task<PagedResponse<CollegeResponse>> ListCollegesAsync(
        long universityId, PageRequest request, CancellationToken token);
}