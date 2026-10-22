using System.ComponentModel.DataAnnotations.Schema;

namespace CsuPlusPlus.Models.ValueObjects;

[ComplexType]
public record NonEmptyName
{
    public string Value { get; private set; }

    public NonEmptyName(string value)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(value);
        if (value.Length > 30)
            throw new ArgumentOutOfRangeException(nameof(value), "名称长度大于30个字符。");
        
        Value = value;
    }
}