using CsuPlusPlus.Dtos.University;
using CsuPlusPlus.Infra;
using CsuPlusPlus.Models.ValueObjects;
using Microsoft.EntityFrameworkCore;

namespace CsuPlusPlus.Services.University;

public class UniversityService(AppDbContext db) : IUniversityService
{
    public async Task<CreateUniversityResult> CreateUniversityAsync(
        CreateUniversityRequest request,
        CancellationToken cancellationToken)
    {
        var name = new NonEmptyName(request.Name);
        if (await db.Universities.AnyAsync(u => u.Name == name, cancellationToken))
            return new CreateUniversityResult(CreateUniversityStatus.NameAlreadyExists);

        var university = new Models.University
        {
            Name = name
        };
        await db.Universities.AddAsync(university, cancellationToken);

        await db.SaveChangesAsync(cancellationToken);

        return new CreateUniversityResult(CreateUniversityStatus.Success,
            new UniversityResponse(university.Name.Value, university.Emblem?.GuidFilename.ToString()));
    }
}