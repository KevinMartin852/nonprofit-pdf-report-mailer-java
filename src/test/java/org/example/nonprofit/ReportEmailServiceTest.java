package org.example.nonprofit;

import java.time.LocalDate;
import java.util.List;

public final class ReportEmailServiceTest {
    public static void main(String[] args) {
        NonprofitReport donation = new NonprofitReport(
                NonprofitReport.Kind.DONOR_RECEIPT,
                "GIFT-42",
                LocalDate.of(2026, 9, 18),
                List.of(new NonprofitReport.Line("Donation", "USD 75.00")));

        ReportEmailService.PreparedReport report = ReportEmailService.prepare(donation);

        check(report.subject().equals("Your donation receipt GIFT-42"), "donations choose receipt language");
        check(report.fileName().equals("receipt-GIFT-42.pdf"), "donations choose a receipt filename");
        check(report.displayLines().contains("Donation: USD 75.00"), "the amount reaches the PDF model");
        check(report.emailHtml().contains("learning programs"), "the donor note reflects the nonprofit mission");
        System.out.println("PASS: donor input selects a receipt PDF and donor email");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
