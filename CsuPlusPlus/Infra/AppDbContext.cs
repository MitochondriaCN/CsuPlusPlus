using CsuPlusPlus.Models;
using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Storage.ValueConversion;
using File = CsuPlusPlus.Models.File;

namespace CsuPlusPlus.Infra;

public class AppDbContext : DbContext
{
    public AppDbContext(DbContextOptions<AppDbContext> options) : base(options)
    {
    }

    public DbSet<College> Colleges { get; set; }
    public DbSet<Course> Courses { get; set; }
    public DbSet<File> Files { get; set; }
    public DbSet<University> Universities { get; set; }
    public DbSet<User> Users { get; set; }
    public DbSet<Setting> Settings { get; set; }

    protected override void ConfigureConventions(ModelConfigurationBuilder configurationBuilder)
    {
        if (Database.ProviderName == "Microsoft.EntityFrameworkCore.Sqlite")
            configurationBuilder.Properties<DateTimeOffset>()
                .HaveConversion<DateTimeOffsetToStringConverter>();
    }
}