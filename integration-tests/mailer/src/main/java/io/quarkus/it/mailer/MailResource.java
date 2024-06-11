package io.quarkus.it.mailer;

import java.io.IOException;
import java.time.Duration;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.MailTemplate;
import io.quarkus.mailer.Mailer;
import io.quarkus.mailer.MailerName;
import io.quarkus.qute.CheckedTemplate;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.buffer.Buffer;

@Path("/mail")
@Produces(MediaType.TEXT_PLAIN)
public class MailResource {

    @Inject
    Mailer defaultMailer;

    @MailerName("start-tls-legacy")
    Mailer startTlsLegacyMailer;

    @MailerName("start-tls-registry")
    Mailer startTlsRegistryMailer;

    @MailerName("start-tls-registry-trust-all")
    Mailer startTlsRegistryMailerWithTrustAll;

    @MailerName("start-tls-legacy-trust-all")
    Mailer startTlsLegacyMailerWithTrustAll;

    @MailerName("tls-legacy")
    Mailer tlsLegacyMailer;

    @MailerName("tls-registry")
    Mailer tlsRegistryMailer;

    @MailerName("tls-legacy-trust-all")
    Mailer tlsLegacyMailerWithTrustAll;

    @MailerName("tls-registry-trust-all")
    Mailer tlsRegistryMailerWithTrustAll;

    private Mailer select(String name) {
        if (name == null) {
            return defaultMailer;
        }
        return switch (name) {
            case "start-tls-legacy" -> startTlsLegacyMailer;
            case "start-tls-registry" -> startTlsRegistryMailer;
            case "start-tls-registry-trust-all" -> startTlsRegistryMailerWithTrustAll;
            case "start-tls-legacy-trust-all" -> startTlsLegacyMailerWithTrustAll;
            case "tls-legacy" -> tlsLegacyMailer;
            case "tls-registry" -> tlsRegistryMailer;
            case "tls-legacy-trust-all" -> tlsLegacyMailerWithTrustAll;
            case "tls-registry-trust-all" -> tlsRegistryMailerWithTrustAll;
            default -> throw new IllegalArgumentException("Unknown mailer " + name);
        };
    }

    @Inject
    Vertx vertx;

    @CheckedTemplate
    static class Templates {
        public static native MailTemplate.MailTemplateInstance hello(String name);
    }

    /**
     * Send a simple text email.
     */
    @GET
    @Path("/text")
    public String sendSimpleTextEmail() {
        return sendSimpleTextEmailWithCustomMailer(null);
    }

    /**
     * Send a simple text email.
     */
    @GET
    @Path("/text/{name}")
    public String sendSimpleTextEmailWithCustomMailer(@PathParam("name") String name) {
        Mailer mailer = select(name);
        String subject = "simple test email";
        if (name != null) {
            subject += " " + name;
        }
        mailer.send(Mail.withText("nobody@quarkus.io",
                subject,
                "This is a simple test email.\nRegards,\nRoger the robot"));
        return "ok";
    }

    /**
     * Send a simple text email with a text attachment (not inlined)
     */
    @GET
    @Path("/text-with-attachment")
    public String sendSimpleTextEmailWithASingleAttachment() {
        Buffer lorem = vertx.fileSystem().readFile("META-INF/resources/lorem.txt").await().atMost(Duration.ofSeconds(1));
        defaultMailer.send(Mail.withText("nobody@quarkus.io",
                "simple test email with an attachment",
                "This is a simple test email.\nRegards,\nRoger the robot")
                .addAttachment("lorem.txt", lorem.getBytes(), "text/plain"));
        return "ok";
    }

    /**
     * Send a simple text email with non-ascii characters.
     */
    @GET
    @Path("/text-non-ascii")
    public String sendSimpleTextEmailWithNonAsciiCharacters() {
        defaultMailer.send(Mail.withText("nobody@quarkus.io",
                "Příliš žluťoučký kůň úpěl ďábelské ódy na 各分野最高のライブラリと標準で構成された、",
                "This is a simple test email with non-ascii characters.\n" +
                        "Non-ascii characters: Příliš žluťoučký kůň úpěl ďábelské ódy na 各分野最高のライブラリと標準で構成された、\n" +
                        "Regards,\nRoger the robot"));
        return "ok";
    }

    /**
     * Send a simple text email with human friendly address prefix.
     */
    @GET
    @Path("/human-friendly-address")
    public String sendEmailWithHumanFriendlyAddressPrefix() {
        defaultMailer.send(Mail.withText("Mr. Nobody <nobody@quarkus.io>",
                "Simple test email",
                "This is a simple test email.\nRegards,\nRoger the robot")
                .setFrom("Roger the robot <roger-the-robot@quarkus.io>"));
        return "ok";
    }

    /**
     * Send a simple HTML email.
     */
    @GET
    @Path("/html")
    public String sendSimpleHtmlEmail() {
        defaultMailer.send(Mail.withHtml("nobody@quarkus.io",
                "html test email",
                "<h3>Hello!</h3><p>This is a simple test email.</p><p>Regards,</p><p>Roger the robot</p>"));
        return "ok";
    }

    /**
     * Send a simple HTML email with inline attachment.
     */
    @GET
    @Path("/html-inline-attachment")
    public String sendSimpleHtmlEmailWithAttachmentInline() throws IOException {
        Buffer logo = vertx.fileSystem().readFile("META-INF/resources/logo.png").await().atMost(Duration.ofSeconds(1));
        defaultMailer.send(Mail.withHtml("nobody@quarkus.io",
                "HTML test email",
                "<h3>Hello!</h3><p>This is a simple test email.</p>" +
                        "<p>Here is a file for you: <img src=\"cid:my-file@quarkus.io\"/></p>" +
                        "<p>Regards,</p><p>Roger the robot</p>")
                .addInlineAttachment("quarkus-logo.png", logo.getBytes(), "image/png",
                        "<my-file@quarkus.io>"));
        return "ok";
    }

    /**
     * Send a simple text email to multiple recipients.
     */
    @GET
    @Path("/multiple-recipients")
    public String sendSimpleTextEmailToMultipleRecipients() {
        defaultMailer.send(Mail.withText("nobody@quarkus.io",
                "simple test email",
                "This is a simple test email.\nRegards,\nRoger the robot")
                .addTo("nobody@example.com"));
        return "ok";
    }

    /**
     * Send a simple text email with DKIM signature.
     */
    @GET
    @Path("/dkim")
    public String sendSimpleEmailWithDkimSignature() {
        defaultMailer.send(Mail.withText("nobody@quarkus.io",
                "simple test email",
                "This is a simple test email.\nRegards,\nRoger the robot")
                .addTo("nobody@example.com"));
        return System.getProperty("vertx.mail.attachment.cache.file");
    }

    /**
     * Confirm attachment caching has been enabled successfully.
     */
    @GET
    @Path("/attachments/cache")
    public String checkAttachmentCache() {
        return System.getProperty("vertx.mail.attachment.cache.file", "false");
    }

    /**
     * Send a text email from template.
     */
    @GET
    @Path("/text-from-template")
    public String sendEmailFromTemplate() {
        Templates.hello("John")
                .subject("template mail")
                .to("nobody@quarkus.io")
                .send()
                .await().atMost(Duration.ofSeconds(10));
        return "ok";
    }

    /**
     * Send a simple text email with custom header.
     */
    @GET
    @Path("/text-with-headers")
    public String sendSimpleTextEmailWithCustomHeader() {
        defaultMailer.send(Mail.withText("nobody@quarkus.io",
                "simple test email",
                "This is a simple test email.\nRegards,\nRoger the robot")
                .addHeader("Accept", "http"));
        return "ok";
    }

}
