using System.Text.Json;

namespace CsuPlusPlus.Dtos.Setting;

public record SettingResponse(
    string Key,
    JsonElement Value);