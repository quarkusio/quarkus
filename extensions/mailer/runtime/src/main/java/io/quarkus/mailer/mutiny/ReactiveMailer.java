package io.quarkus.mailer.mutiny;

import java.util.concurrent.CompletionStage;

import io.quarkus.mailer.Mail;
import io.smallrye.mutiny.Uni;

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
    Uni<Void> send(Mail... mails);
}
