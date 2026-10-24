using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace CsuPlusPlus.Infra.Migrations.Postgres;

/// <inheritdoc />
public partial class _20261024054526_AddUserRole : Migration
{
    /// <inheritdoc />
    protected override void Up(MigrationBuilder migrationBuilder)
    {
        migrationBuilder.AddColumn<int>(
            name: "Role",
            table: "users",
            type: "integer",
            nullable: false,
            defaultValue: 0);
    }

    /// <inheritdoc />
    protected override void Down(MigrationBuilder migrationBuilder)
    {
        migrationBuilder.DropColumn(
            name: "Role",
            table: "users");
    }
}
