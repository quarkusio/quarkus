package io.quarkus.mailer;

import java.time.Duration;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.mailer.reactive.ReactiveMailer;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Uni;

public class InvalidEmailTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(root -> root
                    .addClasses(Sender.class));

    @Inject
    MockMailbox mockMailbox;

    @Inject
    Sender sender;

    @Test
    public void testInvalidTo() {
        List<String> to = List.of("clement@test.io", "inv alid@quarkus.io", "max@test.io");
        List<String> cc = List.of();
        List<String> bcc = List.of();
        Assertions.assertThatThrownBy(() -> sender.send(to, cc, bcc).await().atMost(Duration.ofSeconds(5)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unable to send an email, an email address is invalid")
                .hasMessageNotContaining("@");
        Assertions.assertThat(mockMailbox.getTotalMessagesSent()).isEqualTo(0);
    }

    @Test
    public void testInvalidCC() {
        List<String> cc = List.of("clement@test.io", "inv alid@quarkus.io", "max@test.io");
        List<String> to = List.of();
        List<String> bcc = List.of();
        Assertions.assertThatThrownBy(() -> sender.send(to, cc, bcc).await().atMost(Duration.ofSeconds(5)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unable to send an email, an email address is invalid")
                .hasMessageNotContaining("@");
        Assertions.assertThat(mockMailbox.getTotalMessagesSent()).isEqualTo(0);
    }

    @Test
    public void testInvalidBCC() {
        List<String> bcc = List.of("clement@test.io", "inv alid@quarkus.io", "max@test.io");
        List<String> to = List.of();
        List<String> cc = List.of();
        Assertions.assertThatThrownBy(() -> sender.send(to, cc, bcc).await().atMost(Duration.ofSeconds(5)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unable to send an email, an email address is invalid")
                .hasMessageNotContaining("@");
        Assertions.assertThat(mockMailbox.getTotalMessagesSent()).isEqualTo(0);
    }

    @Singleton
    static class Sender {

        @Inject
        ReactiveMailer mailer;

        Uni<Void> send(List<String> to, List<String> cc, List<String> bcc) {
            Mail mail = new Mail()
                    .setTo(to)
                    .setCc(cc)
                    .setBcc(bcc)
                    .setSubject("Test")
                    .setText("Hello!");
            return mailer.send(mail);
        }
    }
}
