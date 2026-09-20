package org.example.nonprofit;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ReportEmailService {
    private final InfraiConfig config;
    private final ReportPdfWriter pdfWriter;
    private final InfraiClient infrai;

    public ReportEmailService(InfraiConfig config, ReportPdfWriter pdfWriter, InfraiClient infrai) {
        this.config = config;
        this.pdfWriter = pdfWriter;
        this.infrai = infrai;
    }

    public Delivery generateAndNotify(NonprofitReport source) throws IOException, InterruptedException {
        PreparedReport prepared = prepare(source);
        Path pdf = pdfWriter.write(config.reportDirectory(), prepared);
        Map<String, String> message = new LinkedHashMap<>();
        message.put("to", config.recipient());
        message.put("subject", prepared.subject());
        message.put("html", prepared.emailHtml());
        // The public call-site idiom is intentionally visible: infrai.email.send
        InfraiClient.SendResult sent = infrai.email.send(message, "nonprofit-report-" + source.reference());
        return new Delivery(pdf, sent.messageId());
    }

    public static PreparedReport prepare(NonprofitReport source) {
        String title;
        String subject;
        String introduction;
        String prefix;
        switch (source.kind()) {
            case DONOR_RECEIPT -> {
                title = "Donor receipt " + source.reference();
                subject = "Your donation receipt " + source.reference();
                introduction = "Thank you for supporting our learning programs. Your receipt PDF has been generated.";
                prefix = "receipt";
            }
            case VOLUNTEER_REMINDER -> {
                title = "Volunteer reminder " + source.reference();
                subject = "Volunteer session reminder " + source.reference();
                introduction = "Your upcoming learning-session details are ready in the generated PDF.";
                prefix = "volunteer-reminder";
            }
            case CAMPAIGN_REPORT -> {
                title = "Campaign report " + source.reference();
                subject = "Learning campaign report " + source.reference();
                introduction = "The campaign reporting PDF has been generated for your review.";
                prefix = "campaign-report";
            }
            default -> throw new IllegalStateException("Unexpected report kind");
        }
        List<String> display = new ArrayList<>();
        display.add("Date: " + source.date());
        source.lines().forEach(line -> display.add(line.label() + ": " + line.value()));
        String html = "<h1>" + escapeHtml(title) + "</h1><p>" + escapeHtml(introduction) + "</p>"
                + "<p>Reference: <strong>" + escapeHtml(source.reference()) + "</strong></p>";
        return new PreparedReport(title, subject, html, prefix + "-" + safe(source.reference()) + ".pdf", display);
    }

    private static String safe(String value) { return value.replaceAll("[^A-Za-z0-9._-]", "-"); }
    private static String escapeHtml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    public record PreparedReport(String title, String subject, String emailHtml, String fileName,
                                 List<String> displayLines) {}
    public record Delivery(Path pdf, String messageId) {}
}
