using CsuPlusPlus.Dtos.Auth;

namespace CsuPlusPlus.Services.Auth;

public interface IAuthService
{
    Task<RegisterResult> RegisterAsync(RegisterRequest request, CancellationToken cancellationToken = default);

    Task<LoginResponse?> LoginAsync(LoginRequest request, CancellationToken cancellationToken = default);
}