using System.Security.Claims;
using System.Text;
using System.Threading.RateLimiting;
using CsuPlusPlus.Authorization;
using CsuPlusPlus.Infra;
using CsuPlusPlus.Models;
using CsuPlusPlus.Options;
using CsuPlusPlus.Services.Auth;
using CsuPlusPlus.Services.College;
using CsuPlusPlus.Services.File;
using CsuPlusPlus.Services.Setting;
using CsuPlusPlus.Services.University;
using CsuPlusPlus.Services.UserProfile;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
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
        builder.Services.Configure<FileStorageOptions>(
            builder.Configuration.GetSection(FileStorageOptions.SectionName));

        // Services
        builder.Services.AddScoped<IPasswordHasher<User>, PasswordHasher<User>>();
        builder.Services.AddScoped<IAuthService, MailPasswordAuthService>();
        builder.Services.AddScoped<IUserProfileService, UserProfileService>();
        builder.Services.AddScoped<IUniversityService, UniversityService>();
        builder.Services.AddScoped<ICollegeService, CollegeService>();
        builder.Services.AddScoped<ISettingsService, JsonSettingsService>();
        builder.Services.AddScoped<IFileService, FileService>();

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
            options.AddPolicy(Policies.ManageSettings, policy =>
                policy.RequireRole(nameof(UserRole.Admin)));
        });

        // Controllers
        builder.Services.AddControllers();

        // Rate limiting
        builder.Services.AddRateLimiter(options =>
        {
            options.RejectionStatusCode = StatusCodes.Status429TooManyRequests;

            options.AddPolicy(RateLimitPolicies.FileDownload, httpContext =>
                RateLimitPartition.GetSlidingWindowLimiter(
                    partitionKey: httpContext.User.FindFirstValue(JwtRegisteredClaimNames.Sub)
                                  ?? httpContext.Connection.RemoteIpAddress?.ToString()
                                  ?? "anonymous",
                    factory: _ => new SlidingWindowRateLimiterOptions
                    {
                        PermitLimit = 5,
                        Window = TimeSpan.FromMinutes(1),
                        SegmentsPerWindow = 5,
                        QueueLimit = 0
                    }));

            options.OnRejected = async (context, cancellationToken) =>
            {
                context.HttpContext.Response.StatusCode = StatusCodes.Status429TooManyRequests;
                context.HttpContext.Response.ContentType = "application/problem+json";
                await context.HttpContext.Response.WriteAsJsonAsync(new ProblemDetails
                {
                    Status = StatusCodes.Status429TooManyRequests,
                    Title = "请求过于频繁，请稍后重试。"
                }, cancellationToken);
            };
        });

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
        app.UseRateLimiter();

        app.MapControllers();

        await app.RunAsync();
    }
}