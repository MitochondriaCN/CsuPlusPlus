using System.ComponentModel.DataAnnotations.Schema;
using CsuPlusPlus.Models.ValueObjects;
using Microsoft.EntityFrameworkCore;

namespace CsuPlusPlus.Models;

[Table("universities")]
[Index(nameof(Name.Value), IsUnique = true)]
public class University
{
    public long Id { get; set; }

    public required NonEmptyName Name { get; set; }

    public File? Emblem { get; set; }
}