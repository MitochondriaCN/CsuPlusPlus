using System.ComponentModel.DataAnnotations.Schema;
using CsuPlusPlus.Models.ValueObjects;
using Microsoft.EntityFrameworkCore;

namespace CsuPlusPlus.Models;

[Table("users")]
[Index("MailAddress.Value", IsUnique = true)]
public class User
{
    public long Id { get; set; }

    public required College College { get; set; }

    public required MailAddress MailAddress { get; set; }

    public required Nickname Nickname { get; set; }

    public required string PasswordHash { get; set; }
}