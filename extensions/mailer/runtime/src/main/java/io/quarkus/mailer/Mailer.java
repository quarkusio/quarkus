package io.quarkus.mailer;

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
