package org.example.nonprofit;

import java.time.LocalDate;
import java.util.List;

public final class NonprofitReportApplication {
    private NonprofitReportApplication() {}

    public static void main(String[] args) throws Exception {
        InfraiConfig config = InfraiConfig.fromEnvironment();
        ReportEmailService service = new ReportEmailService(config, new ReportPdfWriter(), new InfraiClient(config));
        NonprofitReport receipt = new NonprofitReport(
                NonprofitReport.Kind.DONOR_RECEIPT,
                "DON-2026-1042",
                LocalDate.of(2026, 9, 18),
                List.of(
                        new NonprofitReport.Line("Donor", "Alex Rivera"),
                        new NonprofitReport.Line("Program", "Community reading lab"),
                        new NonprofitReport.Line("Donation", "USD 125.00")));
        ReportEmailService.Delivery result = service.generateAndNotify(receipt);
        System.out.println("Generated PDF: " + result.pdf());
        System.out.println("Email accepted with message_id: " + result.messageId());
    }
}
