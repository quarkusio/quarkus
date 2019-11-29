package io.quarkus.mailer.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.mail.MessagingException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.mailer.Mail;
import io.vertx.axle.core.Vertx;

class MockMailerImplTest {

    private static final String FROM = "test@test.org";
    private static final String TO = "foo@quarkus.io";

    private static Vertx vertx;
    private ReactiveMailerImpl mailer;

    @BeforeAll
    static void start() {
        vertx = Vertx.vertx();
    }

    @AfterAll
    static void stop() {
        vertx.close().toCompletableFuture().join();
    }

    @BeforeEach
    void init() {
        mailer = new ReactiveMailerImpl();
        mailer.configure(Optional.of(FROM), Optional.empty(), true);
        mailer.vertx = vertx;
        mailer.mockMailbox = new MockMailboxImpl();

        mailer.mockMailbox.clear();
    }

    @Test
    void testTextMail() throws MessagingException, IOException {
        String content = UUID.randomUUID().toString();
        mailer.send(Mail.withText(TO, "Test", content)).toCompletableFuture().join();

        List<Mail> sent = mailer.mockMailbox.getMessagesSentTo(TO);
        assertThat(sent).hasSize(1);
        Mail actual = sent.get(0);
        assertThat(actual.getText()).contains(content);
        assertThat(actual.getSubject()).isEqualTo("Test");
    }

    @Test
    void testWithSeveralMails() {
        Mail mail1 = Mail.withText(TO, "Mail 1", "Mail 1").addCc("cc@quarkus.io").addBcc("bcc@quarkus.io");
        Mail mail2 = Mail.withHtml(TO, "Mail 2", "<strong>Mail 2</strong>").addCc("cc2@quarkus.io").addBcc("bcc2@quarkus.io");
        mailer.send(mail1, mail2).toCompletableFuture().join();
        assertThat(mailer.mockMailbox.getTotalMessagesSent()).isEqualTo(6);
    }
}
