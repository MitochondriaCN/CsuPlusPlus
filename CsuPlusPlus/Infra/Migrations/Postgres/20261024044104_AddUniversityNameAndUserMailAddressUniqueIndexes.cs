#nullable disable

using Microsoft.EntityFrameworkCore.Migrations;

namespace CsuPlusPlus.Infra.Migrations.Postgres;

/// <inheritdoc />
public partial class _20261024044104_AddUniversityNameAndUserMailAddressUniqueIndexes : Migration
{
    /// <inheritdoc />
    protected override void Up(MigrationBuilder migrationBuilder)
    {
        migrationBuilder.DropForeignKey(
            name: "FK_files_courses_CourseId",
            table: "files");

        migrationBuilder.AddColumn<long>(
            name: "EmblemId",
            table: "universities",
            type: "bigint",
            nullable: true);

        migrationBuilder.AlterColumn<long>(
            name: "CourseId",
            table: "files",
            type: "bigint",
            nullable: true,
            oldClrType: typeof(long),
            oldType: "bigint");

        migrationBuilder.CreateIndex(
            name: "IX_users_MailAddress_Value",
            table: "users",
            column: "MailAddress_Value",
            unique: true);

        migrationBuilder.CreateIndex(
            name: "IX_universities_EmblemId",
            table: "universities",
            column: "EmblemId");

        migrationBuilder.CreateIndex(
            name: "IX_universities_Name_Value",
            table: "universities",
            column: "Name_Value",
            unique: true);

        migrationBuilder.AddForeignKey(
            name: "FK_files_courses_CourseId",
            table: "files",
            column: "CourseId",
            principalTable: "courses",
            principalColumn: "Id");

        migrationBuilder.AddForeignKey(
            name: "FK_universities_files_EmblemId",
            table: "universities",
            column: "EmblemId",
            principalTable: "files",
            principalColumn: "Id");
    }

    /// <inheritdoc />
    protected override void Down(MigrationBuilder migrationBuilder)
    {
        migrationBuilder.DropForeignKey(
            name: "FK_files_courses_CourseId",
            table: "files");

        migrationBuilder.DropForeignKey(
            name: "FK_universities_files_EmblemId",
            table: "universities");

        migrationBuilder.DropIndex(
            name: "IX_users_MailAddress_Value",
            table: "users");

        migrationBuilder.DropIndex(
            name: "IX_universities_EmblemId",
            table: "universities");

        migrationBuilder.DropIndex(
            name: "IX_universities_Name_Value",
            table: "universities");

        migrationBuilder.DropColumn(
            name: "EmblemId",
            table: "universities");

        migrationBuilder.AlterColumn<long>(
            name: "CourseId",
            table: "files",
            type: "bigint",
            nullable: false,
            defaultValue: 0L,
            oldClrType: typeof(long),
            oldType: "bigint",
            oldNullable: true);

        migrationBuilder.AddForeignKey(
            name: "FK_files_courses_CourseId",
            table: "files",
            column: "CourseId",
            principalTable: "courses",
            principalColumn: "Id",
            onDelete: ReferentialAction.Cascade);
    }
}