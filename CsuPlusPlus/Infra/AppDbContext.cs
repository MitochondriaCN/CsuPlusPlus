using CsuPlusPlus.Models;
using Microsoft.EntityFrameworkCore;
using File = CsuPlusPlus.Models.File;

namespace CsuPlusPlus.Infra;

public class AppDbContext : DbContext
{
    public DbSet<College> Colleges { get; set; }
    public DbSet<Course> Courses { get; set; }
    public DbSet<File> Files { get; set; }
    public DbSet<University> Universities { get; set; }
    public DbSet<User> Users { get; set; }

    public AppDbContext(DbContextOptions<AppDbContext> options) : base(options)
    {
    }
}