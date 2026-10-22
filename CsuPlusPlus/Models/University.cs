using System.ComponentModel.DataAnnotations.Schema;
using CsuPlusPlus.Models.ValueObjects;

namespace CsuPlusPlus.Models;

[Table("universities")]
public class University
{
    public long Id { get; set; }
    
    public required NonEmptyName Name { get; set; }
}