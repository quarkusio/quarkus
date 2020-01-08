package io.quarkus.mailer;

/**
 * A mailer to send email.
 *
 * @see io.quarkus.mailer.mutiny.ReactiveMailer
 */
public interface Mailer {

    /**
     * Sends the given mails.
     *
     * @param mails the mails, must not be {@code null}.
     */
    void send(Mail... mails);

}
