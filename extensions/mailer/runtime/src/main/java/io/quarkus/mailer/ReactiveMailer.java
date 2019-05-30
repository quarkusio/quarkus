package io.quarkus.mailer;

import java.util.concurrent.CompletionStage;

/**
 * A mailer to send email asynchronously.
 */
public interface ReactiveMailer {

    /**
     * Sends the passed emails.
     *
     * @param mails the emails to send, must not be {@code null}
     * @return a {@link CompletionStage} indicating when the mails have been sent. The {@link CompletionStage} may be
     *         completed with a failure if the emails cannot be sent.
     */
    CompletionStage<Void> send(Mail... mails);
}
