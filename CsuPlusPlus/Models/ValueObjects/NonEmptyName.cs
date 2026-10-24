using System.ComponentModel.DataAnnotations.Schema;

namespace CsuPlusPlus.Models.ValueObjects;

[ComplexType]
public record NonEmptyName
{
    public const int MaxLength = 30;

    public NonEmptyName(string value)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(value);
        if (value.Length > MaxLength)
            throw new ArgumentOutOfRangeException(nameof(value), "名称长度大于30个字符。");

        Value = value;
    }

    public string Value { get; private set; }
}