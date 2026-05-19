
### Sending Emails

```java
@Inject Mailer mailer;

// Plain text
mailer.send(Mail.withText("to@example.com", "Subject", "Body text"));

// HTML
mailer.send(Mail.withHtml("to@example.com", "Subject", "<h1>Hello</h1>"));
```

- `Mailer` is the blocking (imperative) API. Use with `@Blocking` on REST endpoints.
- `ReactiveMailer` is the non-blocking API — `send()` returns `Uni<Void>`.
- Import `io.quarkus.mailer.Mailer` and `io.quarkus.mailer.Mail`.

### Reactive API

```java
@Inject io.quarkus.mailer.reactive.ReactiveMailer reactiveMailer;

public Uni<Void> sendAsync(String to, String subject, String body) {
    return reactiveMailer.send(Mail.withText(to, subject, body));
}
```

Import `io.quarkus.mailer.reactive.ReactiveMailer`.

### Mail Builder

```java
Mail mail = Mail.withText("alice@example.com", "Report", "Plain text body")
    .addCc("bob@example.com")
    .addBcc("admin@example.com")
    .setHtml("<h1>HTML body</h1>")
    .addHeader("X-Custom-Header", "value")
    .setFrom("noreply@example.com")
    .setReplyTo("support@example.com");
```

### Attachments

```java
// File attachment
mail.addAttachment("report.pdf", pdfBytes, "application/pdf");

// Inline attachment for HTML (referenced as <img src="cid:logo">)
mail.addInlineAttachment("logo.png", pngBytes, "image/png", "logo");
```

### Qute Email Templates

With the `qute` extension, use type-safe email templates. The `@CheckedTemplate` must be a **nested static class** inside your resource/service:

```java
@ApplicationScoped
public class EmailService {

    @CheckedTemplate
    static class Templates {
        public static native MailTemplate.MailTemplateInstance welcome(String name, String link);
    }

    public void sendWelcome(String to, String name, String link) {
        Templates.welcome(name, link)
            .to(to)
            .subject("Welcome!")
            .send().await().indefinitely();
    }
}
```

Template file at `src/main/resources/templates/EmailService/welcome.html`:
```html
<h1>Welcome {name}!</h1>
<a href="{link}">Activate your account</a>
```

The template path follows `{EnclosingClass}/{methodName}.html`.

### Configuration

```properties
# Default "from" address
quarkus.mailer.from=noreply@example.com

# SMTP settings (for production — not needed with Dev Services)
quarkus.mailer.host=smtp.example.com
quarkus.mailer.port=587
quarkus.mailer.start-tls=REQUIRED
quarkus.mailer.username=user
quarkus.mailer.password=pass
```

### Dev Services

In dev and test mode, emails are captured by a **mock mailer** — they are logged to the console but NOT actually sent. No SMTP server needed.

### Testing with MockMailbox

```java
@QuarkusTest
class MailTest {
    @Inject MockMailbox mailbox;

    @BeforeEach
    void clear() { mailbox.clear(); }

    @Test
    void testSendEmail() {
        // trigger email sending via REST or service call
        given().contentType(JSON).body(request).post("/send").then().statusCode(200);

        List<MailMessage> sent = mailbox.getMailMessagesSentTo("alice@example.com");
        assertEquals(1, sent.size());
        assertEquals("Subject", sent.get(0).getSubject());
        assertTrue(sent.get(0).getText().contains("expected text"));
    }
}
```

- Use `getMailMessagesSentTo(address)` (NOT the deprecated `getMessagesSentTo()`).
- Call `mailbox.clear()` in `@BeforeEach` for test isolation.
- `MailMessage` has `getSubject()`, `getText()`, `getHtml()`, `getTo()`, `getAttachments()`.

### Common Pitfalls

- Imperative `Mailer` requires `@Blocking` on REST endpoints — it blocks the event loop otherwise.
- In dev/test mode, emails are mocked by default — check console logs, not your inbox.
- `MockMailbox.getMessagesSentTo()` is deprecated — use `getMailMessagesSentTo()` instead.
- Set `quarkus.mailer.from` for production — dev mode uses a default sender.
