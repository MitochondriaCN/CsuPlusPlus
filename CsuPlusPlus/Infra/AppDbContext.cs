using Microsoft.EntityFrameworkCore;

namespace CsuPlusPlus.Infra;

public class AppDbContext : DbContext
{
    public AppDbContext(DbContextOptions<AppDbContext> options) : base(options)
    {
    }
    
    

}