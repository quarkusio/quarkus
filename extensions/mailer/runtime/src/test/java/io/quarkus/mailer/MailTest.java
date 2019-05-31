package io.quarkus.mailer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.*;

import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.vertx.axle.core.Vertx;

class MailTest {

    private static final File LOREM = new File("src/test/resources/lorem-ipsum.txt");
    private static final String BEGINNING = "Sed ut perspiciatis unde omnis iste natus error sit";
    private static final String DESCRIPTION = "my lorem ipsum";
    private static final String TO_ADDRESS = "quarkus@quarkus.io";
    private static final String TEXT_PLAIN = "text/plain";
    private static Vertx vertx;

    @BeforeAll
    static void init() {
        vertx = Vertx.vertx();
    }

    @AfterAll
    static void closing() {
        vertx.close().toCompletableFuture().join();
    }

    @Test
    void testSimpleTextEmail() {
        Mail mail = new Mail().addTo(TO_ADDRESS).setSubject("test").setText(BEGINNING)
                .setFrom("from@quarkus.io")
                .setReplyTo("reply-to@quarkus.io")
                .setBounceAddress("bounce@quarkus.io");
        assertThat(mail.getTo()).containsExactly(TO_ADDRESS);
        assertThat(mail.getSubject()).isEqualTo("test");
        assertThat(mail.getText()).isEqualTo(BEGINNING);
        assertThat(mail.getHtml()).isNull();
        assertThat(mail.getFrom()).isEqualTo("from@quarkus.io");
        assertThat(mail.getReplyTo()).isEqualTo("reply-to@quarkus.io");
        assertThat(mail.getBounceAddress()).isEqualTo("bounce@quarkus.io");
    }

    @Test
    void testSimpleHTMLEmail() {
        Mail mail = new Mail().addTo(TO_ADDRESS).setSubject("test").setHtml(BEGINNING);
        assertThat(mail.getTo()).containsExactly(TO_ADDRESS);
        assertThat(mail.getSubject()).isEqualTo("test");
        assertThat(mail.getText()).isNull();
        assertThat(mail.getHtml()).isEqualTo(BEGINNING);
    }

    @Test
    void testMailWithMultipleReceivers() {
        Mail mail = new Mail()
                .addTo("to1@quarkus.io", "to2@quarkus.io")
                .addCc("cc1@quarkus.io", "cc2@quarkus.io")
                .addBcc("bcc1@quarkus.io", "bcc2@quarkus.io");

        assertThat(mail.getTo()).containsExactly("to1@quarkus.io", "to2@quarkus.io");
        assertThat(mail.getCc()).containsExactly("cc1@quarkus.io", "cc2@quarkus.io");
        assertThat(mail.getBcc()).containsExactly("bcc1@quarkus.io", "bcc2@quarkus.io");
    }

    @Test
    void testMailWithMultipleReceiversSet() {
        Mail mail = new Mail()
                .setTo(Arrays.asList("to1@quarkus.io", "to2@quarkus.io"))
                .setCc(Arrays.asList("cc1@quarkus.io", "cc2@quarkus.io"))
                .setBcc(Arrays.asList("bcc1@quarkus.io", "bcc2@quarkus.io"));

        assertThat(mail.getTo()).containsExactly("to1@quarkus.io", "to2@quarkus.io");
        assertThat(mail.getCc()).containsExactly("cc1@quarkus.io", "cc2@quarkus.io");
        assertThat(mail.getBcc()).containsExactly("bcc1@quarkus.io", "bcc2@quarkus.io");
    }

    @Test
    void testMailWithReceiversSetWithNull() {
        Mail mail = new Mail()
                .setTo(null)
                .setCc(null)
                .setBcc(null);

        assertThat(mail.getTo()).isEmpty();
        assertThat(mail.getCc()).isEmpty();
        assertThat(mail.getBcc()).isEmpty();
    }

    @Test
    void testMailWithAttachments() {
        Mail mail1 = new Mail().addAttachment("some-name-1", LOREM, TEXT_PLAIN);
        assertThat(mail1.getAttachments()).hasSize(1);
        assertThat(mail1.getAttachments().get(0).getName()).isEqualTo("some-name-1");
        assertThat(mail1.getAttachments().get(0).getContentType()).isEqualTo(TEXT_PLAIN);
        assertThat(mail1.getAttachments().get(0).getFile()).isFile();
        assertThat(mail1.getAttachments().get(0).getDisposition()).isEqualTo(Attachment.DISPOSITION_ATTACHMENT);

        Mail mail2 = new Mail().addAttachment("some-name-2", BEGINNING.getBytes(StandardCharsets.UTF_8), TEXT_PLAIN);
        assertThat(mail2.getAttachments()).hasSize(1);
        assertThat(mail2.getAttachments().get(0).getName()).isEqualTo("some-name-2");
        assertThat(mail2.getAttachments().get(0).getContentType()).isEqualTo(TEXT_PLAIN);
        assertThat(mail2.getAttachments().get(0).getData()).isNotNull();
        assertThat(mail2.getAttachments().get(0).getDisposition()).isEqualTo(Attachment.DISPOSITION_ATTACHMENT);

        Mail mail3 = new Mail().addAttachment("some-name-3",
                ReactiveStreams.of(0, 1, 2).map(Integer::byteValue).buildRs(), TEXT_PLAIN);
        assertThat(mail3.getAttachments()).hasSize(1);
        assertThat(mail3.getAttachments().get(0).getName()).isEqualTo("some-name-3");
        assertThat(mail3.getAttachments().get(0).getContentType()).isEqualTo(TEXT_PLAIN);
        assertThat(mail3.getAttachments().get(0).getData()).isNotNull();
        assertThat(mail3.getAttachments().get(0).getDisposition()).isEqualTo(Attachment.DISPOSITION_ATTACHMENT);

        Mail mail4 = new Mail().addAttachment("some-name-4",
                ReactiveStreams.of(0, 1, 2).map(Integer::byteValue).buildRs(), TEXT_PLAIN,
                DESCRIPTION, Attachment.DISPOSITION_ATTACHMENT);
        assertThat(mail4.getAttachments()).hasSize(1);
        assertThat(mail4.getAttachments().get(0).getName()).isEqualTo("some-name-4");
        assertThat(mail4.getAttachments().get(0).getContentType()).isEqualTo(TEXT_PLAIN);
        assertThat(mail4.getAttachments().get(0).getData()).isNotNull();
        assertThat(mail4.getAttachments().get(0).getDescription()).isEqualTo(DESCRIPTION);
        assertThat(mail4.getAttachments().get(0).getDisposition()).isEqualTo(Attachment.DISPOSITION_ATTACHMENT);

        Mail mail5 = new Mail().addAttachment("some-name-5", BEGINNING.getBytes(StandardCharsets.UTF_8), TEXT_PLAIN,
                DESCRIPTION, Attachment.DISPOSITION_ATTACHMENT);
        assertThat(mail5.getAttachments()).hasSize(1);
        assertThat(mail5.getAttachments().get(0).getName()).isEqualTo("some-name-5");
        assertThat(mail5.getAttachments().get(0).getContentType()).isEqualTo(TEXT_PLAIN);
        assertThat(mail5.getAttachments().get(0).getData()).isNotNull();
        assertThat(mail5.getAttachments().get(0).getDescription()).isEqualTo(DESCRIPTION);
        assertThat(mail5.getAttachments().get(0).getDisposition()).isEqualTo(Attachment.DISPOSITION_ATTACHMENT);
    }

    @Test
    void testMailWithInlineAttachments() {
        Mail mail1 = new Mail().addInlineAttachment("name-1", LOREM, TEXT_PLAIN, "cid-1");
        assertThat(mail1.getAttachments()).hasSize(1);
        assertThat(mail1.getAttachments().get(0).getName()).isEqualTo("name-1");
        assertThat(mail1.getAttachments().get(0).getContentType()).isEqualTo(TEXT_PLAIN);
        assertThat(mail1.getAttachments().get(0).getFile()).isFile();
        assertThat(mail1.getAttachments().get(0).getDisposition()).isEqualTo(Attachment.DISPOSITION_INLINE);
        assertThat(mail1.getAttachments().get(0).getContentId()).isEqualTo("<cid-1>");

        Mail mail2 = new Mail().addInlineAttachment("name-2", BEGINNING.getBytes(StandardCharsets.UTF_8), TEXT_PLAIN, "cid-2");
        assertThat(mail2.getAttachments()).hasSize(1);
        assertThat(mail2.getAttachments().get(0).getName()).isEqualTo("name-2");
        assertThat(mail2.getAttachments().get(0).getContentType()).isEqualTo(TEXT_PLAIN);
        assertThat(mail2.getAttachments().get(0).getDisposition()).isEqualTo(Attachment.DISPOSITION_INLINE);
        assertThat(mail2.getAttachments().get(0).getContentId()).isEqualTo("<cid-2>");

        Mail mail3 = new Mail().addInlineAttachment("name-3",
                ReactiveStreams.of(0, 1, 2).map(Integer::byteValue).buildRs(), TEXT_PLAIN, "cid-3");
        assertThat(mail3.getAttachments()).hasSize(1);
        assertThat(mail3.getAttachments().get(0).getName()).isEqualTo("name-3");
        assertThat(mail3.getAttachments().get(0).getContentType()).isEqualTo(TEXT_PLAIN);
        assertThat(mail3.getAttachments().get(0).getData()).isNotNull();
        assertThat(mail3.getAttachments().get(0).getDisposition()).isEqualTo(Attachment.DISPOSITION_INLINE);
        assertThat(mail3.getAttachments().get(0).getContentId()).isEqualTo("<cid-3>");
    }

    @Test
    void testHeaders() {
        Mail mail = new Mail()
                .addHeader("foo", "bar").addHeader("multiple", "a", "b", "c");
        assertThat(mail.getHeaders()).hasSize(2).contains(entry("foo", Collections.singletonList("bar")),
                entry("multiple", Arrays.asList("a", "b", "c")));

        mail.removeHeader("foo");
        assertThat(mail.getHeaders()).hasSize(1).containsKeys("multiple");

        Map<String, List<String>> headers = Collections.singletonMap("X-header", Collections.singletonList("foo"));
        mail.setHeaders(headers);
        assertThat(mail.getHeaders()).hasSize(1).containsKeys("X-header");

        mail.setHeaders(null);
        assertThat(mail.getHeaders()).isEmpty();
    }

}
