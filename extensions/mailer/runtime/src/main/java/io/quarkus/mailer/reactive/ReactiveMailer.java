package io.quarkus.mailer.reactive;

import io.quarkus.mailer.Mail;
import io.smallrye.mutiny.Uni;

/**
 * A mailer to send email asynchronously.
 */
public interface ReactiveMailer {

    /**
     * Sends the given emails.
     *
     * @param mails the emails to send, must not be {@code null}, must not contain {@code null}
     * @return a {@link Uni} indicating when the mails have been sent. The {@link Uni} may fire a failure if the
     *         emails cannot be sent.
     */
    Uni<Void> send(Mail... mails);
}
