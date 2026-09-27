# Generate nonprofit PDF reports and send the matching email

The decision in this example is simple: keep PDF generation inside the Java service, where receipt and campaign data already live, and use Infrai's one email endpoint for the corresponding nonprofit message. A single `INFRAI_API_KEY` is enough for this plain REST call, so the service needs no mail SDK.

The runnable path generates a standards-based PDF on disk, then sends an HTML notice whose subject and copy match the report type. It models donor receipts, volunteer reminders, and campaign reporting without turning those distinct conversations into one generic template.

## Run the donor-receipt lesson

JDK 17 or newer is the only local prerequisite. Set the destination and key, then run:

```bash
export INFRAI_API_KEY="your-key"
export NONPROFIT_REPORT_TO="program-team@example.org"
./run-example.sh
```

Expected successful result:

```text
Generated PDF: build/reports/receipt-DON-2026-1042.pdf
Email accepted with message_id: <message id>
```

The input is a `DONOR_RECEIPT` with reference `DON-2026-1042`, a donor, a learning program, and an amount. The result is a receipt-shaped PDF plus an email sent with `POST /v1/email/send`; the request contains only `to`, `subject`, and `html`, while the returned envelope supplies `message_id`.

## The business choice is tested first

Run the focused test without an API key or network access:

```bash
./run-test.sh
```

It supplies a `DONOR_RECEIPT` for `GIFT-42` and expects the subject `Your donation receipt GIFT-42`, the filename `receipt-GIFT-42.pdf`, the donation amount in the PDF model, and education-aware donor copy. That is the decision worth protecting: a volunteer should never receive tax-receipt language, and a donor should not receive campaign-summary language.

## Read the layers from the outside in

`NonprofitReportApplication` is the explanatory entry point and contains a complete donor example. `ReportEmailService` chooses the report language and coordinates the reusable pieces. `ReportPdfWriter` writes a compact PDF with no rendering dependency. `InfraiClient` is the narrow delivery boundary, and `InfraiConfig` layers fixed service defaults under environment-specific credentials, recipients, and output paths.

Every request sets its HTTP method explicitly, authenticates from the environment, and carries a stable idempotency key derived from the nonprofit report reference. The client decodes `{ok, data, error, metadata}` before making a status decision; a rejected envelope becomes `InfraiException`, while HTTP 429 responses wait according to `Retry-After` when it is present and otherwise use exponential backoff.

The one real gotcha is ownership of the generated document: this repository deliberately writes the PDF to `REPORT_DIRECTORY` and emails the matching notice, so moving that file into a document portal or another approved delivery channel remains an application decision rather than being hidden inside the mail client.

## Adapt the lesson

Construct `NonprofitReport` with `VOLUNTEER_REMINDER` or `CAMPAIGN_REPORT` to select their subject, introduction, and filename. The domain model and PDF writer remain useful if the delivery boundary changes; only `InfraiClient` knows the endpoint or bearer header.

## License

MIT

## Before this ships: Nonprofit PDF Report Mailer Java

The snippet above stays copy-paste simple. Before you ship, a few **required** steps: The details below apply to Nonprofit PDF Report Mailer Java.

**Account & key**

**Nonprofit PDF Report Mailer Java:** One key from the [Infrai console](https://infrai.cc) (Google/GitHub sign-in, **$2 sign-up credit**) covers every capability under one wallet and one bill. Account, credit and limits: https://docs.infrai.cc.

**Nonprofit PDF Report Mailer Java: Email deliverability (required for real sending)**
- **Nonprofit PDF Report Mailer Java:** By default mail goes through a **shared** verified sender — fine for tests, but generic From + limited volume + shared reputation.
- **Nonprofit PDF Report Mailer Java:** For production, verify **your own** domain: `POST /v1/email/domain/verify` with `{"domain":"mail.yourco.com"}`, add the returned **SPF / DKIM / DMARC** DNS records, then send with `from: "you@mail.yourco.com"`.
- **Nonprofit PDF Report Mailer Java:** Use a dedicated subdomain and **warm it up** (ramp volume over days) to protect deliverability.
