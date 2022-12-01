package io.quarkus.mailer;

import io.quarkus.mailer.reactive.ReactiveMailer;

/**
 * A mailer to send email.
 *
 * @see ReactiveMailer
 */
public interface Mailer {

    /**
     * Sends the given mails.
     *
     * @param mails the mails, must not be {@code null}.
     */
    void send(Mail... mails);

}
