using System.Globalization;
using System.Security.Claims;
using System.Text;
using CsuPlusPlus.Dtos.Auth;
using CsuPlusPlus.Infra;
using CsuPlusPlus.Models;
using CsuPlusPlus.Models.ValueObjects;
using CsuPlusPlus.Options;
using Microsoft.AspNetCore.Identity;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Options;
using Microsoft.IdentityModel.JsonWebTokens;
using Microsoft.IdentityModel.Tokens;

namespace CsuPlusPlus.Services.Auth;

public class MailPasswordAuthService(
    AppDbContext db,
    IPasswordHasher<User> passwordHasher,
    IOptions<JwtOptions> jwtOptions) : IAuthService
{
    public async Task<RegisterResult> RegisterAsync(
        RegisterRequest request, CancellationToken cancellationToken = default)
    {
        var email = NormalizeEmail(request.Email);
        MailAddress mailAddress;
        Nickname nickname;
        try
        {
            mailAddress = new MailAddress(email);
            nickname = new Nickname(request.Nickname);
        }
        catch (ArgumentException)
        {
            return new RegisterResult(RegisterStatus.InvalidProfile);
        }

        if (await db.Users.AnyAsync(u => u.MailAddress.Value.ToLower() == email, cancellationToken))
            return new RegisterResult(RegisterStatus.EmailAlreadyExists);

        var college = await db.Colleges.SingleOrDefaultAsync(
            c => c.Id == request.CollegeId, cancellationToken);
        if (college is null)
            return new RegisterResult(RegisterStatus.CollegeNotFound);

        var user = new User
        {
            College = college,
            MailAddress = mailAddress,
            Nickname = nickname,
            PasswordHash = string.Empty
        };
        user.PasswordHash = passwordHasher.HashPassword(user, request.Password);

        // 验证签名配置
        GetSigningCredentials();

        db.Users.Add(user);
        await db.SaveChangesAsync(cancellationToken);

        return new RegisterResult(RegisterStatus.Success, IssueToken(user));
    }

    public async Task<LoginResponse?> LoginAsync(
        LoginRequest request, CancellationToken cancellationToken = default)
    {
        var email = NormalizeEmail(request.Email);
        var user = await db.Users.Include(u => u.College)
            .SingleOrDefaultAsync(u => u.MailAddress.Value.ToLower() == email, cancellationToken);
        if (user is null)
            return null;

        var verification = passwordHasher.VerifyHashedPassword(user, user.PasswordHash, request.Password);
        if (verification == PasswordVerificationResult.Failed)
            return null;

        if (verification == PasswordVerificationResult.SuccessRehashNeeded)
        {
            user.PasswordHash = passwordHasher.HashPassword(user, request.Password);
            await db.SaveChangesAsync(cancellationToken);
        }

        return IssueToken(user);
    }

    private LoginResponse IssueToken(User user)
    {
        var options = jwtOptions.Value;
        var now = DateTimeOffset.UtcNow;
        var expiresAt = now.AddMinutes(options.AccessTokenMinutes);
        var token = new JsonWebTokenHandler().CreateToken(new SecurityTokenDescriptor
        {
            Issuer = options.Issuer,
            Audience = options.Audience,
            IssuedAt = now.UtcDateTime,
            NotBefore = now.UtcDateTime,
            Expires = expiresAt.UtcDateTime,
            SigningCredentials = GetSigningCredentials(),
            Subject = new ClaimsIdentity([
                new Claim(JwtRegisteredClaimNames.Sub, user.Id.ToString(CultureInfo.InvariantCulture)),
                new Claim(JwtRegisteredClaimNames.Email, user.MailAddress.Value),
                new Claim(JwtRegisteredClaimNames.Jti, Guid.NewGuid().ToString("N"))
            ])
        });

        return new LoginResponse(token, expiresAt, UserProfileDto.From(user));
    }

    private SigningCredentials GetSigningCredentials()
    {
        var options = jwtOptions.Value;
        if (string.IsNullOrWhiteSpace(options.Issuer)
            || string.IsNullOrWhiteSpace(options.Audience)
            || Encoding.UTF8.GetByteCount(options.Secret) < 32
            || options.AccessTokenMinutes <= 0)
            throw new InvalidOperationException(
                "请配置 Jwt:Issuer、Jwt:Audience、至少 32 字节的 Jwt:Secret 和正数 Jwt:AccessTokenMinutes。");

        return new SigningCredentials(
            new SymmetricSecurityKey(Encoding.UTF8.GetBytes(options.Secret)), SecurityAlgorithms.HmacSha256);
    }

    private static string NormalizeEmail(string email) => email.Trim().ToLowerInvariant();
}