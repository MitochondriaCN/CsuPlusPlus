namespace CsuPlusPlus.Authorization;

public static class Policies
{
    public const string ManageAnyUniversity = "University.Manage";

    public const string ManageOwnUniversity = "OwnUniversity.Manage";

    public const string ManageAnyCourse = "Course.Manage";

    public const string ManageSettings = "Settings.Manage";

    public const string UploadGlobalFile = "File.UploadGlobally";

    public const string DownloadGlobalFile = "File.DownloadGlobally";

    public const string AccessOwnUniversity = "AccessOwnUniversity";
}