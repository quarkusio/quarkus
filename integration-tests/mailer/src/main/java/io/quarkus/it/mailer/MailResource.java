package io.quarkus.it.mailer;

import java.time.Duration;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.MailTemplate;
import io.quarkus.mailer.Mailer;
import io.quarkus.qute.CheckedTemplate;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.buffer.Buffer;

@Path("/mail")
@Produces(MediaType.TEXT_PLAIN)
public class MailResource {

    @Inject
    Mailer mailer;

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
        mailer.send(Mail.withText("nobody@quarkus.io",
                "simple test email",
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
        mailer.send(Mail.withText("nobody@quarkus.io",
                "simple test email with an attachment",
                "This is a simple test email.\nRegards,\nRoger the robot")
                .addAttachment("lorem.txt", lorem.getBytes(), "text/plain"));
        return "ok";
    }

    /**
     * Send a simple HTML email.
     */
    @GET
    @Path("/html")
    public String sendSimpleHtmlEmail() {
        mailer.send(Mail.withHtml("nobody@quarkus.io",
                "html test email",
                "<h3>Hello!</h3><p>This is a simple test email.</p><p>Regards,</p><p>Roger the robot</p>"));
        return "ok";
    }

    /**
     * Send a simple text email to multiple recipients.
     */
    @GET
    @Path("/multiple-recipients")
    public String sendSimpleTextEmailToMultipleRecipients() {
        mailer.send(Mail.withText("nobody@quarkus.io",
                "simple test email",
                "This is a simple test email.\nRegards,\nRoger the robot")
                .addTo("nobody@example.com"));
        return "ok";
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
                .await().indefinitely();
        return "ok";
    }

    /**
     * Send a simple text email with custom header.
     */
    @GET
    @Path("/text-with-headers")
    public String sendSimpleTextEmailWithCustomHeader() {
        mailer.send(Mail.withText("nobody@quarkus.io",
                "simple test email",
                "This is a simple test email.\nRegards,\nRoger the robot")
                .addHeader("Accept", "http"));
        return "ok";
    }

}
