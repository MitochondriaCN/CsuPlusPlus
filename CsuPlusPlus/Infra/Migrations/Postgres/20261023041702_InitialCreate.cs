#nullable disable

using Microsoft.EntityFrameworkCore.Migrations;
using Npgsql.EntityFrameworkCore.PostgreSQL.Metadata;

namespace CsuPlusPlus.Infra.Migrations.Postgres
{
    /// <inheritdoc />
    public partial class InitialCreate : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.CreateTable(
                name: "universities",
                columns: table => new
                {
                    Id = table.Column<long>(type: "bigint", nullable: false)
                        .Annotation("Npgsql:ValueGenerationStrategy",
                            NpgsqlValueGenerationStrategy.IdentityByDefaultColumn),
                    Name_Value = table.Column<string>(type: "text", nullable: false)
                },
                constraints: table => { table.PrimaryKey("PK_universities", x => x.Id); });

            migrationBuilder.CreateTable(
                name: "colleges",
                columns: table => new
                {
                    Id = table.Column<long>(type: "bigint", nullable: false)
                        .Annotation("Npgsql:ValueGenerationStrategy",
                            NpgsqlValueGenerationStrategy.IdentityByDefaultColumn),
                    UniversityId = table.Column<long>(type: "bigint", nullable: false),
                    Name_Value = table.Column<string>(type: "text", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_colleges", x => x.Id);
                    table.ForeignKey(
                        name: "FK_colleges_universities_UniversityId",
                        column: x => x.UniversityId,
                        principalTable: "universities",
                        principalColumn: "Id",
                        onDelete: ReferentialAction.Cascade);
                });

            migrationBuilder.CreateTable(
                name: "courses",
                columns: table => new
                {
                    Id = table.Column<long>(type: "bigint", nullable: false)
                        .Annotation("Npgsql:ValueGenerationStrategy",
                            NpgsqlValueGenerationStrategy.IdentityByDefaultColumn),
                    UniversityId = table.Column<long>(type: "bigint", nullable: false),
                    CollegeId = table.Column<long>(type: "bigint", nullable: true),
                    Hours = table.Column<long>(type: "bigint", nullable: true),
                    CreatedAt = table.Column<DateTimeOffset>(type: "timestamp with time zone", nullable: false),
                    UpdatedAt = table.Column<DateTimeOffset>(type: "timestamp with time zone", nullable: false),
                    Alias_Value = table.Column<string>(type: "text", nullable: true),
                    Name_Value = table.Column<string>(type: "text", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_courses", x => x.Id);
                    table.ForeignKey(
                        name: "FK_courses_colleges_CollegeId",
                        column: x => x.CollegeId,
                        principalTable: "colleges",
                        principalColumn: "Id");
                    table.ForeignKey(
                        name: "FK_courses_universities_UniversityId",
                        column: x => x.UniversityId,
                        principalTable: "universities",
                        principalColumn: "Id",
                        onDelete: ReferentialAction.Cascade);
                });

            migrationBuilder.CreateTable(
                name: "users",
                columns: table => new
                {
                    Id = table.Column<long>(type: "bigint", nullable: false)
                        .Annotation("Npgsql:ValueGenerationStrategy",
                            NpgsqlValueGenerationStrategy.IdentityByDefaultColumn),
                    CollegeId = table.Column<long>(type: "bigint", nullable: false),
                    PasswordHash = table.Column<string>(type: "text", nullable: false),
                    MailAddress_Value = table.Column<string>(type: "text", nullable: false),
                    Nickname_Value = table.Column<string>(type: "text", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_users", x => x.Id);
                    table.ForeignKey(
                        name: "FK_users_colleges_CollegeId",
                        column: x => x.CollegeId,
                        principalTable: "colleges",
                        principalColumn: "Id",
                        onDelete: ReferentialAction.Cascade);
                });

            migrationBuilder.CreateTable(
                name: "files",
                columns: table => new
                {
                    Id = table.Column<long>(type: "bigint", nullable: false)
                        .Annotation("Npgsql:ValueGenerationStrategy",
                            NpgsqlValueGenerationStrategy.IdentityByDefaultColumn),
                    GuidFilename = table.Column<Guid>(type: "uuid", nullable: false),
                    UpdaterId = table.Column<long>(type: "bigint", nullable: false),
                    CourseId = table.Column<long>(type: "bigint", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_files", x => x.Id);
                    table.ForeignKey(
                        name: "FK_files_courses_CourseId",
                        column: x => x.CourseId,
                        principalTable: "courses",
                        principalColumn: "Id",
                        onDelete: ReferentialAction.Cascade);
                    table.ForeignKey(
                        name: "FK_files_users_UpdaterId",
                        column: x => x.UpdaterId,
                        principalTable: "users",
                        principalColumn: "Id",
                        onDelete: ReferentialAction.Cascade);
                });

            migrationBuilder.CreateIndex(
                name: "IX_colleges_UniversityId",
                table: "colleges",
                column: "UniversityId");

            migrationBuilder.CreateIndex(
                name: "IX_courses_CollegeId",
                table: "courses",
                column: "CollegeId");

            migrationBuilder.CreateIndex(
                name: "IX_courses_UniversityId",
                table: "courses",
                column: "UniversityId");

            migrationBuilder.CreateIndex(
                name: "IX_files_CourseId",
                table: "files",
                column: "CourseId");

            migrationBuilder.CreateIndex(
                name: "IX_files_UpdaterId",
                table: "files",
                column: "UpdaterId");

            migrationBuilder.CreateIndex(
                name: "IX_users_CollegeId",
                table: "users",
                column: "CollegeId");
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropTable(
                name: "files");

            migrationBuilder.DropTable(
                name: "courses");

            migrationBuilder.DropTable(
                name: "users");

            migrationBuilder.DropTable(
                name: "colleges");

            migrationBuilder.DropTable(
                name: "universities");
        }
    }
}