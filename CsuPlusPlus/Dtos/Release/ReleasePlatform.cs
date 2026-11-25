using System.Text.Json.Serialization;

namespace CsuPlusPlus.Dtos.Release;

[JsonConverter(typeof(JsonStringEnumConverter))]
public enum ReleasePlatform
{
    Android,
    MacOs,
    Windows,
    Linux,
    Ios
}