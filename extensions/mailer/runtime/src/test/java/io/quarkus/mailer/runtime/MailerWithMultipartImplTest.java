package io.quarkus.mailer.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.subethamail.wiser.Wiser;
import org.subethamail.wiser.WiserMessage;

import io.quarkus.mailer.Mail;
import io.smallrye.mutiny.Multi;
import io.vertx.ext.mail.MailConfig;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.mail.MailClient;

/**
 * Same as {@link MailerImplTest} but force all mail to be sent as multipart.
 */
class MailerWithMultipartImplTest {

    private static final String FROM = "test@test.org";
    private static final String TO = "foo@quarkus.io";
    private static final String TEXT_CONTENT_TYPE = "text/plain";

    private static Wiser wiser;
    private static Vertx vertx;
    private MutinyMailerImpl mailer;

    @BeforeAll
    static void startWiser() {
        wiser = new Wiser();
        wiser.setPort(0);
        wiser.start();

        vertx = Vertx.vertx();
    }

    @AfterAll
    static void stopWiser() {
        wiser.stop();
        vertx.close().await().indefinitely();
    }

    @BeforeEach
    void init() {
        mailer = new MutinyMailerImpl();
        mailer.mailerSupport = new MailerSupport(FROM, null, false);
        mailer.vertx = vertx;
        mailer.client = MailClient.createShared(mailer.vertx,
                new MailConfig().setPort(wiser.getServer().getPort()).setMultiPartOnly(true));

        wiser.getMessages().clear();
    }

    @Test
    void testTextMail() throws MessagingException, IOException {
        String uuid = UUID.randomUUID().toString();
        mailer.send(Mail.withText(TO, "Test", uuid)).await().indefinitely();
        assertThat(wiser.getMessages()).hasSize(1);
        WiserMessage actual = wiser.getMessages().get(0);
        assertThat(getContent(actual)).contains(uuid);
        MimeMessage msg = actual.getMimeMessage();
        List<String> types = getContentTypesFromMimeMultipart((MimeMultipart) actual.getMimeMessage().getContent());
        assertThat(types).containsExactly(TEXT_CONTENT_TYPE);
        assertThat(msg.getSubject()).isEqualTo("Test");
        assertThat(msg.getFrom()[0].toString()).isEqualTo(FROM);
        assertThat(msg.getAllRecipients()).hasSize(1).contains(new InternetAddress(TO));
    }

    @Test
    void testHTMLMail() throws MessagingException {
        String content = UUID.randomUUID().toString();
        mailer.send(Mail.withHtml(TO, "Test", "<h1>" + content + "</h1>")).await().indefinitely();
        assertThat(wiser.getMessages()).hasSize(1);
        WiserMessage actual = wiser.getMessages().get(0);
        assertThat(getContent(actual)).contains("<h1>" + content + "</h1>");
        List<String> types = Collections.singletonList(actual.getMimeMessage().getContentType());
        assertThat(types).hasSize(1).allSatisfy(s -> assertThat(s).contains("multipart/mixed"));
        MimeMessage msg = actual.getMimeMessage();
        assertThat(msg.getSubject()).isEqualTo("Test");
        assertThat(msg.getContentType()).startsWith("multipart/mixed");
        assertThat(msg.getFrom()[0].toString()).isEqualTo(FROM);
        assertThat(msg.getAllRecipients()).hasSize(1).contains(new InternetAddress(TO));
    }

    @Test
    void testWithSeveralMails() {
        Mail mail1 = Mail.withText(TO, "Mail 1", "Mail 1").addCc("cc@quarkus.io").addBcc("bcc@quarkus.io");
        Mail mail2 = Mail.withHtml(TO, "Mail 2", "<strong>Mail 2</strong>").addCc("cc2@quarkus.io").addBcc("bcc2@quarkus.io");
        mailer.send(mail1, mail2).await().indefinitely();
        assertThat(wiser.getMessages()).hasSize(6);
    }

    @Test
    void testHeaders() throws MessagingException {
        mailer.send(Mail.withText(TO, "Test", "testHeaders")
                .addHeader("X-header", "value")
                .addHeader("X-header-2", "value1", "value2"))
                .await().indefinitely();
        assertThat(wiser.getMessages()).hasSize(1);
        WiserMessage actual = wiser.getMessages().get(0);
        MimeMessage msg = actual.getMimeMessage();
        assertThat(msg.getSubject()).isEqualTo("Test");
        assertThat(msg.getFrom()[0].toString()).isEqualTo(FROM);
        assertThat(msg.getHeader("X-header")).hasSize(1).contains("value");
        assertThat(msg.getHeader("X-header-2")).hasSize(2).contains("value1", "value2");
    }

    @Test
    void testAttachment() throws MessagingException, IOException {
        String payload = UUID.randomUUID().toString();
        mailer.send(Mail.withText(TO, "Test", "testAttachment")
                .addAttachment("my-file.txt", payload.getBytes(StandardCharsets.UTF_8), TEXT_CONTENT_TYPE))
                .await().indefinitely();
        assertThat(wiser.getMessages()).hasSize(1);
        WiserMessage actual = wiser.getMessages().get(0);
        assertThat(getContent(actual)).contains("testAttachment");
        MimeMessage msg = actual.getMimeMessage();
        assertThat(msg.getSubject()).isEqualTo("Test");
        assertThat(msg.getFrom()[0].toString()).isEqualTo(FROM);
        String value = getAttachment("my-file.txt", (MimeMultipart) actual.getMimeMessage().getContent());
        assertThat(value).isEqualTo(payload);
    }

    @Test
    void testAttachmentAsStream() throws MessagingException, IOException {
        String payload = UUID.randomUUID().toString();
        byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);
        Iterable<Byte> iterable = () -> new Iterator<Byte>() {
            private int index = 0;

            @Override
            public boolean hasNext() {
                return bytes.length > index;
            }

            @Override
            public Byte next() {
                return bytes[index++];
            }
        };

        mailer.send(Mail.withText(TO, "Test", "testAttachmentAsStream")
                .addAttachment("my-file.txt", Multi.createFrom().iterable(iterable), TEXT_CONTENT_TYPE))
                .await().indefinitely();
        assertThat(wiser.getMessages()).hasSize(1);
        WiserMessage actual = wiser.getMessages().get(0);
        assertThat(getContent(actual)).contains("testAttachment");
        MimeMessage msg = actual.getMimeMessage();
        assertThat(msg.getSubject()).isEqualTo("Test");
        assertThat(msg.getFrom()[0].toString()).isEqualTo(FROM);
        String value = getAttachment("my-file.txt", (MimeMultipart) actual.getMimeMessage().getContent());
        assertThat(value).isEqualTo(payload);
    }

    @Test
    void testInlineAttachment() throws MessagingException, IOException {
        String cid = UUID.randomUUID().toString() + "@acme";
        mailer.send(Mail.withHtml(TO, "Test", "testInlineAttachment")
                .addInlineAttachment("inline.txt", "my inlined text".getBytes(StandardCharsets.UTF_8), TEXT_CONTENT_TYPE, cid))
                .await().indefinitely();
        assertThat(wiser.getMessages()).hasSize(1);
        WiserMessage actual = wiser.getMessages().get(0);
        assertThat(getContent(actual)).contains("testInlineAttachment");
        MimeMessage msg = actual.getMimeMessage();
        assertThat(msg.getSubject()).isEqualTo("Test");
        assertThat(msg.getFrom()[0].toString()).isEqualTo(FROM);

        String value = getInlineAttachment("<" + cid + ">", (MimeMultipart) actual.getMimeMessage().getContent());
        assertThat(value).isEqualTo("my inlined text");
    }

    @Test
    void testAttachments() throws MessagingException, IOException {
        mailer.send(Mail.withText(TO, "Test", "Simple Test")
                .addAttachment("some-data.txt", "Hello".getBytes(StandardCharsets.UTF_8), TEXT_CONTENT_TYPE)
                .addAttachment("some-data-2.txt", "Hello 2".getBytes(StandardCharsets.UTF_8), TEXT_CONTENT_TYPE))
                .await().indefinitely();
        assertThat(wiser.getMessages()).hasSize(1);
        WiserMessage actual = wiser.getMessages().get(0);
        assertThat(getContent(actual)).contains("Simple Test");
        MimeMessage msg = actual.getMimeMessage();
        assertThat(msg.getSubject()).isEqualTo("Test");
        assertThat(msg.getFrom()[0].toString()).isEqualTo(FROM);
        String value = getAttachment("some-data.txt", (MimeMultipart) actual.getMimeMessage().getContent());
        assertThat(value).isEqualTo("Hello");
        value = getAttachment("some-data-2.txt", (MimeMultipart) actual.getMimeMessage().getContent());
        assertThat(value).isEqualTo("Hello 2");
    }

    @Test
    void testReplyToHeaderIsSet() throws MessagingException {
        mailer.send(Mail.withText(TO, "Test", "testHeaders")
                .setReplyTo("reply-to@quarkus.io"))
                .await().indefinitely();
        assertThat(wiser.getMessages()).hasSize(1);
        WiserMessage actual = wiser.getMessages().get(0);
        MimeMessage msg = actual.getMimeMessage();
        assertThat(msg.getHeader("Reply-To")).containsExactly("reply-to@quarkus.io");
        assertThat(msg.getReplyTo()).containsExactly(InternetAddress.parse("reply-to@quarkus.io"));
    }

    private String getContent(WiserMessage msg) {
        try {
            Object content = msg.getMimeMessage().getContent();
            if (content instanceof String) {
                return content.toString();
            }
            return getTextFromMimeMultipart((MimeMultipart) content);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private String getAttachment(String name, MimeMultipart multipart) throws IOException, MessagingException {
        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart bodyPart = multipart.getBodyPart(i);
            if (bodyPart.getFileName() != null && bodyPart.getFileName().equalsIgnoreCase(name)) {
                assertThat(bodyPart.getContentType()).startsWith(TEXT_CONTENT_TYPE);
                return read(bodyPart);
            }
        }
        return null;
    }

    private String getInlineAttachment(String cid, MimeMultipart multipart) throws IOException, MessagingException {
        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart bodyPart = multipart.getBodyPart(i);
            if (bodyPart.getContent() instanceof MimeMultipart) {
                for (int j = 0; j < ((MimeMultipart) bodyPart.getContent()).getCount(); j++) {
                    BodyPart nested = ((MimeMultipart) bodyPart.getContent()).getBodyPart(j);
                    if (nested.getHeader("Content-ID") != null && nested.getHeader("Content-ID")[0].equalsIgnoreCase(cid)) {
                        assertThat(nested.getDisposition()).isEqualTo("inline");
                        assertThat(nested.getContentType()).startsWith(TEXT_CONTENT_TYPE);
                        return read(nested);
                    }
                }
            } else if (bodyPart.getContent() instanceof String) {
                if (bodyPart.getHeader("Content-ID") != null && bodyPart.getHeader("Content-ID")[0].equalsIgnoreCase(cid)) {
                    return (String) bodyPart.getContent();
                }
            }
        }
        return null;
    }

    private String read(BodyPart part) throws IOException, MessagingException {
        try (InputStream is = part.getInputStream()) {
            Scanner s = new Scanner(is, "UTF-8").useDelimiter("\\A");
            return s.hasNext() ? s.next() : "";
        }
    }

    private String getTextFromMimeMultipart(
            MimeMultipart mimeMultipart) throws MessagingException, IOException {
        StringBuilder result = new StringBuilder();
        int count = mimeMultipart.getCount();
        for (int i = 0; i < count; i++) {
            BodyPart bodyPart = mimeMultipart.getBodyPart(i);
            if (bodyPart.isMimeType(TEXT_CONTENT_TYPE)) {
                result.append("\n").append(bodyPart.getContent());
                break; // without break same text appears twice in my tests
            } else if (bodyPart.isMimeType("text/html")) {
                result.append("\n").append(bodyPart.getContent());
            } else if (bodyPart.getContent() instanceof MimeMultipart) {
                result.append(getTextFromMimeMultipart((MimeMultipart) bodyPart.getContent()));
            }
        }
        return result.toString();
    }

    private List<String> getContentTypesFromMimeMultipart(
            MimeMultipart mimeMultipart) throws MessagingException, IOException {
        List<String> types = new ArrayList<>();
        int count = mimeMultipart.getCount();
        for (int i = 0; i < count; i++) {
            BodyPart bodyPart = mimeMultipart.getBodyPart(i);
            if (bodyPart.getContent() instanceof MimeMultipart) {
                types.addAll(getContentTypesFromMimeMultipart((MimeMultipart) bodyPart.getContent()));
            } else {
                types.add(bodyPart.getContentType());
            }
        }
        return types;
    }

    private List<String> getContentTypesFromMimeMultipart(
            String content) throws MessagingException, IOException {
        return Collections.singletonList(content);
    }

}
