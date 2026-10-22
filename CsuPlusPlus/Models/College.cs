using System.ComponentModel.DataAnnotations.Schema;
using CsuPlusPlus.Models.ValueObjects;

namespace CsuPlusPlus.Models;

[Table("colleges")]
public class College
{
    public long Id { get; set; }

    public required University University { get; set; }

    public required NonEmptyName Name { get; set; }
}