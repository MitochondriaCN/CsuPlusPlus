using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Design;

namespace CsuPlusPlus.Infra;

public class AppDbContextFactory : IDesignTimeDbContextFactory<AppDbContext>
{
    public AppDbContext CreateDbContext(string[] args)
    {
        var builder = WebApplication.CreateBuilder(args);
        var connectionString = builder.Configuration.GetConnectionString("DefaultConnection")
                               ?? throw new InvalidOperationException("缺少连接字符串 ConnectionStrings:DefaultConnection。");

        var options = new DbContextOptionsBuilder<AppDbContext>();
        if (builder.Environment.IsDevelopment())
            options.UseSqlite(connectionString);
        else
            options.UseNpgsql(connectionString);

        return new AppDbContext(options.Options);
    }
}