using System;
using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace CsuPlusPlus.Infra.Migrations.Postgres;

/// <inheritdoc />
public partial class _20261114080610_AddFileUploadMetadata : Migration
{
    /// <inheritdoc />
    protected override void Up(MigrationBuilder migrationBuilder)
    {
        migrationBuilder.DropForeignKey(
            name: "FK_files_users_UpdaterId",
            table: "files");

        migrationBuilder.RenameColumn(
            name: "UpdaterId",
            table: "files",
            newName: "UploaderId");

        migrationBuilder.RenameIndex(
            name: "IX_files_UpdaterId",
            table: "files",
            newName: "IX_files_UploaderId");

        migrationBuilder.AddColumn<DateTimeOffset>(
            name: "CreatedAt",
            table: "files",
            type: "timestamp with time zone",
            nullable: false,
            defaultValue: new DateTimeOffset(new DateTime(1, 1, 1, 0, 0, 0, 0, DateTimeKind.Unspecified), new TimeSpan(0, 0, 0, 0, 0)));

        migrationBuilder.AddColumn<string>(
            name: "Extension",
            table: "files",
            type: "text",
            nullable: false,
            defaultValue: "");

        migrationBuilder.AddColumn<string>(
            name: "FriendlyFilename",
            table: "files",
            type: "character varying(30)",
            maxLength: 30,
            nullable: true);

        migrationBuilder.AddColumn<byte[]>(
            name: "Sha256",
            table: "files",
            type: "bytea",
            nullable: false,
            defaultValue: new byte[0]);

        migrationBuilder.AlterColumn<DateTimeOffset>(
            name: "CreatedAt",
            table: "files",
            type: "timestamp with time zone",
            nullable: false,
            oldClrType: typeof(DateTimeOffset),
            oldType: "timestamp with time zone",
            oldDefaultValue: new DateTimeOffset(new DateTime(1, 1, 1, 0, 0, 0, 0, DateTimeKind.Unspecified),
                new TimeSpan(0, 0, 0, 0, 0)));

        migrationBuilder.AlterColumn<string>(
            name: "Extension",
            table: "files",
            type: "text",
            nullable: false,
            oldClrType: typeof(string),
            oldType: "text",
            oldDefaultValue: "");

        migrationBuilder.AlterColumn<byte[]>(
            name: "Sha256",
            table: "files",
            type: "bytea",
            nullable: false,
            oldClrType: typeof(byte[]),
            oldType: "bytea",
            oldDefaultValue: new byte[0]);

        migrationBuilder.CreateTable(
            name: "settings",
            columns: table => new
            {
                Key = table.Column<string>(type: "text", nullable: false),
                Value = table.Column<string>(type: "jsonb", nullable: false),
                UpdatedAt = table.Column<DateTimeOffset>(type: "timestamp with time zone", nullable: false)
            },
            constraints: table =>
            {
                table.PrimaryKey("PK_settings", x => x.Key);
            });

        migrationBuilder.CreateIndex(
            name: "IX_files_Sha256",
            table: "files",
            column: "Sha256",
            unique: true);

        migrationBuilder.AddForeignKey(
            name: "FK_files_users_UploaderId",
            table: "files",
            column: "UploaderId",
            principalTable: "users",
            principalColumn: "Id",
            onDelete: ReferentialAction.Cascade);
    }

    /// <inheritdoc />
    protected override void Down(MigrationBuilder migrationBuilder)
    {
        migrationBuilder.DropForeignKey(
            name: "FK_files_users_UploaderId",
            table: "files");

        migrationBuilder.DropTable(
            name: "settings");

        migrationBuilder.DropIndex(
            name: "IX_files_Sha256",
            table: "files");

        migrationBuilder.DropColumn(
            name: "CreatedAt",
            table: "files");

        migrationBuilder.DropColumn(
            name: "Extension",
            table: "files");

        migrationBuilder.DropColumn(
            name: "FriendlyFilename",
            table: "files");

        migrationBuilder.DropColumn(
            name: "Sha256",
            table: "files");

        migrationBuilder.RenameColumn(
            name: "UploaderId",
            table: "files",
            newName: "UpdaterId");

        migrationBuilder.RenameIndex(
            name: "IX_files_UploaderId",
            table: "files",
            newName: "IX_files_UpdaterId");

        migrationBuilder.AddForeignKey(
            name: "FK_files_users_UpdaterId",
            table: "files",
            column: "UpdaterId",
            principalTable: "users",
            principalColumn: "Id",
            onDelete: ReferentialAction.Cascade);
    }
}
