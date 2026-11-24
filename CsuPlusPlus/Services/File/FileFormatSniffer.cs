using System.IO.Compression;
using System.Text;

namespace CsuPlusPlus.Services.File;

public static class FileFormatSniffer
{
    private static readonly byte[] PdfSignature = [0x25, 0x50, 0x44, 0x46];
    private static readonly byte[] ZipSignature = [0x50, 0x4B, 0x03, 0x04];
    private static readonly byte[] PngSignature = [0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A];
    private static readonly byte[] JpegSignature = [0xFF, 0xD8, 0xFF];
    private static readonly byte[] Gif87ASignature = [0x47, 0x49, 0x46, 0x38, 0x37, 0x61];
    private static readonly byte[] Gif89ASignature = [0x47, 0x49, 0x46, 0x38, 0x39, 0x61];
    private static readonly byte[] RiffSignature = [0x52, 0x49, 0x46, 0x46];
    private static readonly byte[] WebpSignature = [0x57, 0x45, 0x42, 0x50];

    public static async Task<FileFormat> DetectAsync(
        Stream stream,
        CancellationToken cancellationToken,
        string? fileName = null)
    {
        var header = new byte[12];
        var read = await stream.ReadAtLeastAsync(header, header.Length, throwOnEndOfStream: false,
            cancellationToken);

        if (read == 0)
            return FileFormat.Unknown;

        var headerSpan = header.AsSpan(0, read);

        if (StartsWith(headerSpan, PdfSignature))
            return FileFormat.Pdf;

        if (StartsWith(headerSpan, PngSignature))
            return FileFormat.Png;

        if (StartsWith(headerSpan, JpegSignature))
            return FileFormat.Jpeg;

        if (StartsWith(headerSpan, Gif87ASignature) || StartsWith(headerSpan, Gif89ASignature))
            return FileFormat.Gif;

        if (read >= 12
            && StartsWith(headerSpan, RiffSignature)
            && headerSpan.Slice(8, WebpSignature.Length).SequenceEqual(WebpSignature))
            return FileFormat.Webp;

        if (StartsWith(headerSpan, ZipSignature))
        {
            if (!stream.CanSeek)
            {
                using var buffered = new MemoryStream();
                buffered.Write(headerSpan);
                await stream.CopyToAsync(buffered, cancellationToken);
                buffered.Position = 0;
                return DetectOfficeDocument(buffered);
            }

            stream.Position = 0;
            return DetectOfficeDocument(stream);
        }

        if (IsMarkdownExtension(fileName)
            && await IsValidUtf8TextAsync(header.AsMemory(0, read), stream, cancellationToken))
            return FileFormat.Markdown;

        return FileFormat.Unknown;
    }

    private static bool StartsWith(ReadOnlySpan<byte> data, ReadOnlySpan<byte> prefix) =>
        data.Length >= prefix.Length && data[..prefix.Length].SequenceEqual(prefix);

    private static bool IsMarkdownExtension(string? fileName)
    {
        if (string.IsNullOrWhiteSpace(fileName))
            return false;

        var extension = Path.GetExtension(fileName);
        return string.Equals(extension, ".md", StringComparison.OrdinalIgnoreCase)
               || string.Equals(extension, ".markdown", StringComparison.OrdinalIgnoreCase);
    }

    private static async Task<bool> IsValidUtf8TextAsync(
        ReadOnlyMemory<byte> initialBytes,
        Stream stream,
        CancellationToken cancellationToken)
    {
        var decoder = new UTF8Encoding(encoderShouldEmitUTF8Identifier: false, throwOnInvalidBytes: true)
            .GetDecoder();
        var buffer = new byte[8192];
        var charBuffer = new char[8192 + 4];

        try
        {
            if (!DecodeAndValidateChunk(decoder, initialBytes.Span, charBuffer, flush: false))
                return false;

            while (true)
            {
                var bytesRead = await stream.ReadAsync(buffer, cancellationToken);
                if (bytesRead == 0)
                    break;

                if (!DecodeAndValidateChunk(decoder, buffer.AsSpan(0, bytesRead), charBuffer, flush: false))
                    return false;
            }

            return DecodeAndValidateChunk(decoder, ReadOnlySpan<byte>.Empty, charBuffer, flush: true);
        }
        catch (DecoderFallbackException)
        {
            return false;
        }
    }

    private static bool DecodeAndValidateChunk(
        Decoder decoder,
        ReadOnlySpan<byte> bytes,
        char[] charBuffer,
        bool flush)
    {
        var remaining = bytes;
        var completed = false;
        while (!completed)
        {
            decoder.Convert(remaining, charBuffer, flush, out var bytesUsed, out var charsUsed, out completed);
            for (var i = 0; i < charsUsed; i++)
            {
                var c = charBuffer[i];
                if ((c < 0x20 && c is not ('\t' or '\n' or '\r')) || c == 0x7F)
                    return false;
            }

            remaining = remaining[bytesUsed..];
        }

        return true;
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
            FileFormat.Markdown => ".md",
            FileFormat.Png => ".png",
            FileFormat.Jpeg => ".jpg",
            FileFormat.Gif => ".gif",
            FileFormat.Webp => ".webp",
            _ => null
        };
}