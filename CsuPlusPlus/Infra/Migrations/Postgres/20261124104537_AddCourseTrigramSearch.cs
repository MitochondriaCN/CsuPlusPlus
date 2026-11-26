#nullable disable

using Microsoft.EntityFrameworkCore.Migrations;

namespace CsuPlusPlus.Infra.Migrations.Postgres;

/// <inheritdoc />
public partial class _20261124104537_AddCourseTrigramSearch : Migration
{
    /// <inheritdoc />
    protected override void Up(MigrationBuilder migrationBuilder)
    {
        migrationBuilder.AlterDatabase()
            .Annotation("Npgsql:PostgresExtension:pg_trgm", ",,");

        migrationBuilder.CreateIndex(
                name: "IX_courses_Alias_Value",
                table: "courses",
                column: "Alias_Value")
            .Annotation("Npgsql:IndexMethod", "gin")
            .Annotation("Npgsql:IndexOperators", new[] { "gin_trgm_ops" });

        migrationBuilder.CreateIndex(
                name: "IX_courses_Name_Value",
                table: "courses",
                column: "Name_Value")
            .Annotation("Npgsql:IndexMethod", "gin")
            .Annotation("Npgsql:IndexOperators", new[] { "gin_trgm_ops" });
    }

    /// <inheritdoc />
    protected override void Down(MigrationBuilder migrationBuilder)
    {
        migrationBuilder.DropIndex(
            name: "IX_courses_Alias_Value",
            table: "courses");

        migrationBuilder.DropIndex(
            name: "IX_courses_Name_Value",
            table: "courses");

        migrationBuilder.AlterDatabase()
            .OldAnnotation("Npgsql:PostgresExtension:pg_trgm", ",,");
    }
}