using CsuPlusPlus.Dtos.College;
using CsuPlusPlus.Dtos.Common;
using CsuPlusPlus.Infra;
using CsuPlusPlus.Models.ValueObjects;
using Microsoft.EntityFrameworkCore;

namespace CsuPlusPlus.Services.College;

public class CollegeService(AppDbContext db) : ICollegeService
{
    public async Task<CreateCollegeResult> CreateCollegeAsync(
        CreateCollegeRequest request,
        CancellationToken token)
    {
        var name = new NonEmptyName(request.Name);

        var university = await db.Universities.FindAsync([request.UniversityId], token);
        if (university is null)
            return new CreateCollegeResult(CreateCollegeStatus.UniversityNotFound);

        if (await db.Colleges.AnyAsync(c => c.University.Id == request.UniversityId && c.Name == name, token))
            return new CreateCollegeResult(CreateCollegeStatus.NameConflictInUniversity);

        var college = new Models.College
        {
            Name = name,
            University = university
        };
        await db.Colleges.AddAsync(college, token);

        await db.SaveChangesAsync(token);

        return new CreateCollegeResult(CreateCollegeStatus.Success,
            new CollegeResponse(college.Id, college.Name.Value, university.Id, university.Name.Value));
    }

    public async Task<PagedResponse<CollegeResponse>> ListCollegesAsync(
        long universityId,
        PageRequest request,
        CancellationToken token)
    {
        var query = db.Colleges
            .AsNoTracking()
            .Where(c => c.University.Id == universityId)
            .OrderBy(c => c.Id);
        var totalCount = await query.LongCountAsync(token);
        var items = await query
            .Skip(request.Skip)
            .Take(request.PageSize)
            .Select(c => new CollegeResponse(
                c.Id,
                c.Name.Value,
                c.University.Id,
                c.University.Name.Value))
            .ToListAsync(token);

        return new PagedResponse<CollegeResponse>(
            Items: items,
            Page: request.Page,
            PageSize: request.PageSize,
            TotalCount: totalCount);
    }
}