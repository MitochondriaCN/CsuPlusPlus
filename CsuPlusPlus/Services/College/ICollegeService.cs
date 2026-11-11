using CsuPlusPlus.Dtos.College;

namespace CsuPlusPlus.Services.College;

public interface ICollegeService
{
    Task<CreateCollegeResult> CreateCollegeAsync(CreateCollegeRequest request, CancellationToken token);
}