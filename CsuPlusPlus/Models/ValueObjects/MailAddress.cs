using System.ComponentModel.DataAnnotations.Schema;
using System.Text.RegularExpressions;

namespace CsuPlusPlus.Models.ValueObjects;

[ComplexType]
public record MailAddress
{
    private static readonly Regex Pattern = new(
        @"^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$",
        RegexOptions.Compiled | RegexOptions.CultureInvariant);

    public string Value { get; private set; }

    public MailAddress(string value)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(value);
        value = value.Trim();
        if (!Pattern.IsMatch(value))
            throw new ArgumentException($"Invalid email address: '{value}'.", nameof(value));
        Value = value;
    }
}