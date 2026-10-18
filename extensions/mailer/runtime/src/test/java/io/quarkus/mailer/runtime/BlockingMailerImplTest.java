package io.quarkus.mailer.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import io.quarkus.mailer.reactive.ReactiveMailer;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Vertx;

class BlockingMailerImplTest {

    private Vertx vertx;

    @BeforeEach
    void setup() {
        vertx = Vertx.vertx();
    }

    @AfterEach
    void tearDown() {
        if (vertx != null) {
            vertx.close().await().indefinitely();
        }
    }

    @Test
    void testTimeoutThrowsException() {
        ReactiveMailer slowMailer = new ReactiveMailer() {
            @Override
            public Uni<Void> send(Mail... mails) {
                return Uni.createFrom().emitter(emitter -> {
                    // Never complete - simulates hanging connection
                });
            }
        };
        Mailer blockingMailer = new BlockingMailerImpl(slowMailer, Duration.ofSeconds(1));
        Mail mail = Mail.withText("test@example.com", "Subject", "Body");

        assertThatThrownBy(() -> blockingMailer.send(mail))
                .isInstanceOf(io.smallrye.mutiny.TimeoutException.class);
    }

    @Test
    void testTimeoutZeroWaitsIndefinitely() throws InterruptedException {
        ReactiveMailer delayedMailer = new ReactiveMailer() {
            @Override
            public Uni<Void> send(Mail... mails) {
                return Uni.createFrom().voidItem()
                        .onItem().delayIt().by(Duration.ofMillis(500));
            }
        };
        Mailer blockingMailer = new BlockingMailerImpl(delayedMailer, Duration.ZERO);
        Mail mail = Mail.withText("test@example.com", "Subject", "Body");

        // Should complete successfully even though it takes 500ms
        // because zero timeout means wait indefinitely
        long startTime = System.currentTimeMillis();
        blockingMailer.send(mail);
        long duration = System.currentTimeMillis() - startTime;

        assertThat(duration).isGreaterThanOrEqualTo(500);
    }

    @Test
    void testTimeoutNullWaitsIndefinitely() {
        ReactiveMailer delayedMailer = new ReactiveMailer() {
            @Override
            public Uni<Void> send(Mail... mails) {
                return Uni.createFrom().voidItem()
                        .onItem().delayIt().by(Duration.ofMillis(500));
            }
        };
        Mailer blockingMailer = new BlockingMailerImpl(delayedMailer, null);
        Mail mail = Mail.withText("test@example.com", "Subject", "Body");

        // Should complete successfully even though it takes 500ms
        // because null timeout means wait indefinitely
        long startTime = System.currentTimeMillis();
        blockingMailer.send(mail);
        long duration = System.currentTimeMillis() - startTime;

        assertThat(duration).isGreaterThanOrEqualTo(500);
    }

    @Test
    void testSuccessfulSendWithinTimeout() {
        ReactiveMailer fastMailer = new ReactiveMailer() {
            @Override
            public Uni<Void> send(Mail... mails) {
                return Uni.createFrom().voidItem();
            }
        };
        Mailer blockingMailer = new BlockingMailerImpl(fastMailer, Duration.ofSeconds(10));
        Mail mail = Mail.withText("test@example.com", "Subject", "Body");

        long startTime = System.currentTimeMillis();
        assertThatCode(() -> blockingMailer.send(mail)).doesNotThrowAnyException();
        long duration = System.currentTimeMillis() - startTime;
        assertThat(duration).isLessThan(1000);
    }

    @Test
    void testExceptionPropagation() {
        ReactiveMailer failingMailer = new ReactiveMailer() {
            @Override
            public Uni<Void> send(Mail... mails) {
                return Uni.createFrom().failure(new RuntimeException("SMTP error"));
            }
        };
        Mailer blockingMailer = new BlockingMailerImpl(failingMailer, Duration.ofSeconds(10));
        Mail mail = Mail.withText("test@example.com", "Subject", "Body");

        assertThatThrownBy(() -> blockingMailer.send(mail))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("SMTP error");
    }
}
