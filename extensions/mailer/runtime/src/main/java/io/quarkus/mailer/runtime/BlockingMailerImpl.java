package io.quarkus.mailer.runtime;

import java.time.Duration;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import io.quarkus.mailer.reactive.ReactiveMailer;

/**
 * Implementation of {@link Mailer} relying on the {@link ReactiveMailer} and waiting for completion.
 */
public class BlockingMailerImpl implements Mailer {

    private final ReactiveMailer mailer;
    private final Duration timeout;

    BlockingMailerImpl(ReactiveMailer mailer, Duration timeout) {
        this.mailer = mailer;
        this.timeout = timeout;
    }

    @Override
    public void send(Mail... mails) {
        if (timeout == null || timeout.isZero()) {
            // Backward compatibility: if timeout is 0 or null, wait indefinitely
            mailer.send(mails).await().indefinitely();
        } else {
            // Use the configured timeout
            mailer.send(mails).await().atMost(timeout);
        }
    }
}
