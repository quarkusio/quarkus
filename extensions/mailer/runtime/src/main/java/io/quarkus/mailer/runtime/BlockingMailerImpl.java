package io.quarkus.mailer.runtime;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import io.quarkus.mailer.reactive.ReactiveMailer;

/**
 * Implementation of {@link Mailer} relying on the {@link ReactiveMailer} and waiting for completion.
 */
public class BlockingMailerImpl implements Mailer {

    private final ReactiveMailer mailer;

    BlockingMailerImpl(ReactiveMailer mailer) {
        this.mailer = mailer;
    }

    @Override
    public void send(Mail... mails) {
        mailer.send(mails).await().indefinitely();
    }
}
