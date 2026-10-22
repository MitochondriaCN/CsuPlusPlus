using System.ComponentModel.DataAnnotations.Schema;

namespace CsuPlusPlus.Models.ValueObjects;

[ComplexType]
public record Nickname
{
    public string Value { get; private set; }

    public Nickname(string value)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(value);
        if (value.Length > 20)
        {
            throw new ArgumentOutOfRangeException(nameof(value), "昵称长度必须小于20。");
        }

        Value = value;
    }
}