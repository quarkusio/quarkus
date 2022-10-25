package io.quarkus.mailer.runtime;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import io.quarkus.mailer.reactive.ReactiveMailer;

/**
 * Implementation of {@link Mailer} relying on the {@link ReactiveMailer} and waiting for completion.
 */
@ApplicationScoped
public class BlockingMailerImpl implements Mailer {

    @Inject
    ReactiveMailer mailer;

    @Override
    public void send(Mail... mails) {
        mailer.send(mails).await().indefinitely();
    }
}
