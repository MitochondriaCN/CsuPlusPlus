namespace CsuPlusPlus.Dtos.Auth;

public record CapabilitiesResponse(IReadOnlyDictionary<string, bool> Capabilities);