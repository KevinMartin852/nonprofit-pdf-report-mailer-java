package org.example.nonprofit;

import java.time.LocalDate;
import java.util.List;

public record NonprofitReport(Kind kind, String reference, LocalDate date, List<Line> lines) {
    public enum Kind { DONOR_RECEIPT, VOLUNTEER_REMINDER, CAMPAIGN_REPORT }
    public record Line(String label, String value) {}

    public NonprofitReport {
        lines = List.copyOf(lines);
        if (reference.isBlank() || lines.isEmpty()) throw new IllegalArgumentException("Report needs content");
    }
}
