### Sending Emails

- Inject `Mailer` (blocking) or `ReactiveMailer` (reactive).
- Send with `mailer.send(Mail.withText("to@example.com", "Subject", "Body"))`.
- Use `Mail.withHtml()` for HTML emails.
- Attach files with `.addAttachment("file.pdf", data, "application/pdf")`.

### Template Emails with Qute

- Create templates in `src/main/resources/templates/`.
- Use `MailTemplate` injection for type-safe template emails.

### Dev Services

- A Mailpit container starts automatically in dev/test — emails are captured for inspection.
- View sent emails at the Mailpit web UI (shown in dev console).

### Testing

- Inject `MockMailbox` in `@QuarkusTest` to capture and assert sent emails.
- `mailbox.getMailMessagesSentTo("to@example.com")` returns captured messages.

### Common Pitfalls

- In production, configure `quarkus.mailer.host`, `port`, `username`, `password`.
- Dev Services Mailpit does NOT send real emails — they are captured locally.
