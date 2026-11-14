using System.Text.Json;
using CsuPlusPlus.Settings;

namespace CsuPlusPlus.Services.Setting;

public interface ISettingsService
{
    Task<T> GetAsync<T>(SettingsKey key, T fallback, CancellationToken cancellationToken);

    Task SetAsync<T>(SettingsKey key, T value, CancellationToken cancellationToken);

    Task<JsonElement> GetWithDefaultAsync(SettingsKey key, CancellationToken cancellationToken);
}