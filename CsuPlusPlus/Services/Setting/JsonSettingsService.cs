using System.Text.Json;
using CsuPlusPlus.Infra;
using CsuPlusPlus.Settings;
using Microsoft.EntityFrameworkCore;

namespace CsuPlusPlus.Services.Setting;

public class JsonSettingsService(AppDbContext db) : ISettingsService
{
    private static readonly JsonSerializerOptions JsonOptions = JsonSerializerOptions.Web;

    public async Task<T> GetAsync<T>(SettingsKey key, T fallback, CancellationToken cancellationToken)
    {
        var json = await db.Settings.AsNoTracking()
            .Where(s => s.Key == key.ToString())
            .Select(s => s.Value)
            .FirstOrDefaultAsync(cancellationToken);

        if (json == null)
            return fallback;

        return JsonSerializer.Deserialize<T>(json, JsonOptions) ?? fallback;
    }

    public async Task SetAsync<T>(SettingsKey key, T value, CancellationToken cancellationToken)
    {
        var json = JsonSerializer.Serialize(value, JsonOptions);
        var storageKey = key.ToString();

        var existing = await db.Settings.FindAsync([storageKey], cancellationToken);
        if (existing == null)
            await db.Settings.AddAsync(new Models.Setting
            {
                Key = storageKey,
                Value = json,
                UpdatedAt = DateTimeOffset.UtcNow
            }, cancellationToken);
        else
        {
            existing.Value = json;
            existing.UpdatedAt = DateTimeOffset.UtcNow;
        }

        await db.SaveChangesAsync(cancellationToken);
    }

    public async Task<JsonElement> GetWithDefaultAsync(SettingsKey key, CancellationToken cancellationToken) =>
        key switch
        {
            SettingsKey.UserFile => ToJson(await GetAsync(key, UserFileSettings.Default, cancellationToken)),
            _ => throw new InvalidOperationException("未知配置键。")
        };

    private static JsonElement ToJson<T>(T value) => JsonSerializer.SerializeToElement(value, JsonOptions);
}