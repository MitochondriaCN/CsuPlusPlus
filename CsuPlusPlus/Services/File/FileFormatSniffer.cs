using System.IO.Compression;

namespace CsuPlusPlus.Services.File;

public static class FileFormatSniffer
{
    private static readonly byte[] PdfSignature = [0x25, 0x50, 0x44, 0x46];
    private static readonly byte[] ZipSignature = [0x50, 0x4B, 0x03, 0x04];

    public static async Task<FileFormat> DetectAsync(Stream stream, CancellationToken cancellationToken)
    {
        var header = new byte[4];
        var read = await stream.ReadAtLeastAsync(header, header.Length, throwOnEndOfStream: false,
            cancellationToken);

        if (read < header.Length)
            return FileFormat.Unknown;

        if (header.AsSpan().SequenceEqual(PdfSignature))
            return FileFormat.Pdf;

        if (!header.AsSpan().SequenceEqual(ZipSignature))
            return FileFormat.Unknown;

        if (!stream.CanSeek)
        {
            using var buffered = new MemoryStream();
            buffered.Write(header);
            await stream.CopyToAsync(buffered, cancellationToken);
            buffered.Position = 0;
            return DetectOfficeDocument(buffered);
        }

        stream.Position = 0;
        return DetectOfficeDocument(stream);
    }

    private static FileFormat DetectOfficeDocument(Stream stream)
    {
        try
        {
            using var archive = new ZipArchive(stream, ZipArchiveMode.Read, leaveOpen: true);
            var names = archive.Entries.Select(e => e.FullName).ToHashSet(StringComparer.OrdinalIgnoreCase);

            if (names.Contains("word/document.xml"))
                return FileFormat.Docx;

            if (names.Contains("ppt/presentation.xml"))
                return FileFormat.Pptx;

            if (names.Contains("xl/workbook.xml"))
                return FileFormat.Xlsx;

            return FileFormat.Zip;
        }
        catch (InvalidDataException)
        {
            return FileFormat.Unknown;
        }
    }

    public static string? ToExtension(this FileFormat format) =>
        format switch
        {
            FileFormat.Pdf => ".pdf",
            FileFormat.Docx => ".docx",
            FileFormat.Pptx => ".pptx",
            FileFormat.Xlsx => ".xlsx",
            FileFormat.Zip => ".zip",
            _ => null
        };
}
