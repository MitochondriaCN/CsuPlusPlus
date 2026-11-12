using System.Text.Json;

namespace CsuPlusPlus.Dtos.Setting;

public record UpdateSettingRequest
{
    public required JsonElement Value { get; init; }
}