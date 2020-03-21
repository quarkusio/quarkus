package io.quarkus.mailer.runtime;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import io.quarkus.mailer.ReactiveMailer;
import io.quarkus.runtime.BlockingOperationControl;

/**
 * Implementation of {@link Mailer} relying on the {@link ReactiveMailer} and waiting for completion.
 */
@ApplicationScoped
public class BlockingMailerImpl implements Mailer {

    @Inject
    ReactiveMailer mailer;

    @Override
    public void send(Mail... mails) {
        if (!BlockingOperationControl.isBlockingAllowed()) {
            throw new RuntimeException(
                    "Attempted a blocking operation from the IO thread. If you want to send mail from an IO thread please use ReactiveMailer instead, or dispatch to a worker thread to use the blocking mailer.");
        }
        mailer.send(mails).toCompletableFuture().join();
    }
}
