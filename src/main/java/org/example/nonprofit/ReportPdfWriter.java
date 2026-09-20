package org.example.nonprofit;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ReportPdfWriter {
    public Path write(Path directory, ReportEmailService.PreparedReport report) throws IOException {
        Files.createDirectories(directory);
        Path target = directory.resolve(report.fileName());
        Files.write(target, pdf(report.title(), report.displayLines()));
        return target;
    }

    private static byte[] pdf(String title, List<String> lines) {
        StringBuilder stream = new StringBuilder("BT\n/F1 18 Tf\n72 740 Td\n")
                .append('(').append(escape(title)).append(") Tj\n/F1 11 Tf\n");
        for (String line : lines) stream.append("0 -24 Td\n(").append(escape(line)).append(") Tj\n");
        stream.append("ET\n");

        List<String> objects = List.of(
                "<< /Type /Catalog /Pages 2 0 R >>",
                "<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
                "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 5 0 R >> >> /Contents 4 0 R >>",
                "<< /Length " + stream.toString().getBytes(StandardCharsets.US_ASCII).length + " >>\nstream\n" + stream + "endstream",
                "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>");
        StringBuilder pdf = new StringBuilder("%PDF-1.4\n");
        List<Integer> offsets = new ArrayList<>();
        for (int i = 0; i < objects.size(); i++) {
            offsets.add(pdf.toString().getBytes(StandardCharsets.US_ASCII).length);
            pdf.append(i + 1).append(" 0 obj\n").append(objects.get(i)).append("\nendobj\n");
        }
        int xref = pdf.toString().getBytes(StandardCharsets.US_ASCII).length;
        pdf.append("xref\n0 6\n0000000000 65535 f \n");
        for (int offset : offsets) pdf.append(String.format("%010d 00000 n \n", offset));
        pdf.append("trailer\n<< /Size 6 /Root 1 0 R >>\nstartxref\n").append(xref).append("\n%%EOF\n");
        return pdf.toString().getBytes(StandardCharsets.US_ASCII);
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)")
                .replaceAll("[^\\x20-\\x7E]", "?");
    }
}
