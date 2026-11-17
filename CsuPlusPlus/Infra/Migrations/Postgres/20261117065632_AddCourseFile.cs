#nullable disable

using Microsoft.EntityFrameworkCore.Migrations;

namespace CsuPlusPlus.Infra.Migrations.Postgres;

/// <inheritdoc />
public partial class _20261117065632_AddCourseFile : Migration
{
    /// <inheritdoc />
    protected override void Up(MigrationBuilder migrationBuilder)
    {
        migrationBuilder.DropForeignKey(
            name: "FK_files_courses_CourseId",
            table: "files");

        migrationBuilder.DropIndex(
            name: "IX_files_CourseId",
            table: "files");

        migrationBuilder.AlterColumn<string>(
            name: "Extension",
            table: "files",
            type: "character varying(10)",
            maxLength: 10,
            nullable: false,
            oldClrType: typeof(string),
            oldType: "text");

        migrationBuilder.CreateTable(
            name: "course_files",
            columns: table => new
            {
                CourseId = table.Column<long>(type: "bigint", nullable: false),
                FileId = table.Column<long>(type: "bigint", nullable: false)
            },
            constraints: table =>
            {
                table.PrimaryKey("PK_course_files", x => new { x.CourseId, x.FileId });
                table.ForeignKey(
                    name: "FK_course_files_courses_CourseId",
                    column: x => x.CourseId,
                    principalTable: "courses",
                    principalColumn: "Id",
                    onDelete: ReferentialAction.Cascade);
                table.ForeignKey(
                    name: "FK_course_files_files_FileId",
                    column: x => x.FileId,
                    principalTable: "files",
                    principalColumn: "Id",
                    onDelete: ReferentialAction.Cascade);
            });

        migrationBuilder.CreateIndex(
            name: "IX_course_files_FileId",
            table: "course_files",
            column: "FileId");

        migrationBuilder.Sql(
            """
            INSERT INTO course_files ("CourseId", "FileId")
            SELECT "CourseId", "Id" FROM files WHERE "CourseId" IS NOT NULL;
            """);

        migrationBuilder.DropColumn(
            name: "CourseId",
            table: "files");
    }

    /// <inheritdoc />
    protected override void Down(MigrationBuilder migrationBuilder)
    {
        migrationBuilder.DropTable(
            name: "course_files");

        migrationBuilder.AlterColumn<string>(
            name: "Extension",
            table: "files",
            type: "text",
            nullable: false,
            oldClrType: typeof(string),
            oldType: "character varying(10)",
            oldMaxLength: 10);

        migrationBuilder.AddColumn<long>(
            name: "CourseId",
            table: "files",
            type: "bigint",
            nullable: true);

        migrationBuilder.CreateIndex(
            name: "IX_files_CourseId",
            table: "files",
            column: "CourseId");

        migrationBuilder.AddForeignKey(
            name: "FK_files_courses_CourseId",
            table: "files",
            column: "CourseId",
            principalTable: "courses",
            principalColumn: "Id");
    }
}