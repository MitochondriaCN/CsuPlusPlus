using System.Text;
using CsuPlusPlus.Authorization;
using CsuPlusPlus.Infra;
using CsuPlusPlus.Models;
using CsuPlusPlus.Options;
using CsuPlusPlus.Services.Auth;
using CsuPlusPlus.Services.University;
using CsuPlusPlus.Services.UserProfile;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.AspNetCore.Identity;
using Microsoft.EntityFrameworkCore;
using Microsoft.IdentityModel.JsonWebTokens;
using Microsoft.IdentityModel.Tokens;
using Microsoft.OpenApi;
using Scalar.AspNetCore;

namespace CsuPlusPlus;

public class Program
{
    public static async Task Main(string[] args)
    {
        var builder = WebApplication.CreateBuilder(args);

        // Add services to the container.

        var connectionString = builder.Configuration.GetConnectionString("DefaultConnection")
                               ?? throw new InvalidOperationException("缺少连接字符串 ConnectionStrings:DefaultConnection。");

        // DB
        if (builder.Environment.IsDevelopment())
            builder.Services.AddDbContext<AppDbContext>(options => options.UseSqlite(connectionString));
        else
            builder.Services.AddDbContext<AppDbContext>(options => options.UseNpgsql(connectionString));

        // Configure
        builder.Services.Configure<JwtOptions>(builder.Configuration.GetSection(JwtOptions.SectionName));

        // Services
        builder.Services.AddScoped<IPasswordHasher<User>, PasswordHasher<User>>();
        builder.Services.AddScoped<IAuthService, MailPasswordAuthService>();
        builder.Services.AddScoped<IUserProfileService, UserProfileService>();
        builder.Services.AddScoped<IUniversityService, UniversityService>();

        // JWT
        var jwt = builder.Configuration
                      .GetSection(JwtOptions.SectionName)
                      .Get<JwtOptions>()
                  ?? throw new InvalidOperationException("缺少 Jwt 配置。");
        if (string.IsNullOrWhiteSpace(jwt.Issuer)
            || string.IsNullOrWhiteSpace(jwt.Audience)
            || Encoding.UTF8.GetByteCount(jwt.Secret) < 32
            || jwt.AccessTokenMinutes <= 0)
        {
            throw new InvalidOperationException(
                "Jwt 配置无效：需提供 Issuer、Audience、至少 32 字节的 Secret 和正数 AccessTokenMinutes。");
        }

        builder.Services
            .AddAuthentication(JwtBearerDefaults.AuthenticationScheme)
            .AddJwtBearer(options =>
            {
                options.MapInboundClaims = false;

                options.TokenValidationParameters = new TokenValidationParameters
                {
                    ValidateIssuer = true,
                    ValidIssuer = jwt.Issuer,

                    ValidateAudience = true,
                    ValidAudience = jwt.Audience,

                    RoleClaimType = "role",
                    NameClaimType = JwtRegisteredClaimNames.Sub,

                    ValidateIssuerSigningKey = true,
                    IssuerSigningKey = new SymmetricSecurityKey(
                        Encoding.UTF8.GetBytes(jwt.Secret)),

                    ValidateLifetime = true,
                    RequireExpirationTime = true,
                    RequireSignedTokens = true,
                    ValidAlgorithms = [SecurityAlgorithms.HmacSha256],

                    ClockSkew = TimeSpan.FromSeconds(30)
                };
            });
        builder.Services.AddAuthorization(options =>
        {
            options.AddPolicy(Policies.ManageUniversity, policy =>
                policy.RequireRole(nameof(UserRole.Admin)));
        });

        // Controllers
        builder.Services.AddControllers();
        // Learn more about configuring OpenAPI at https://aka.ms/aspnet/openapi
        builder.Services.AddOpenApi();
        builder.Services.AddOpenApi("v1-3.1", options =>
        {
            options.OpenApiVersion = OpenApiSpecVersion.OpenApi3_1;
            options.ShouldInclude = description => description.GroupName is null or "v1";
        });

        var app = builder.Build();

        // DB initialization
        await using (var scope = app.Services.CreateAsyncScope())
        {
            var db = scope.ServiceProvider.GetRequiredService<AppDbContext>();
            if (app.Environment.IsDevelopment())
                await db.Database.EnsureCreatedAsync();
            else
                await db.Database.MigrateAsync();
        }

        // Middlewares

        app.MapOpenApi();
        if (app.Environment.IsDevelopment())
            app.MapScalarApiReference();

        app.UseHttpsRedirection();

        app.UseAuthentication();
        app.UseAuthorization();

        app.MapControllers();

        await app.RunAsync();
    }
}