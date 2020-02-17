package io.quarkus.mailer.runtime;

import java.util.concurrent.CompletionStage;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.reactive.ReactiveMailer;

@ApplicationScoped
public class ReactiveMailerImpl implements io.quarkus.mailer.ReactiveMailer {

    @Inject
    ReactiveMailer client;

    @Override
    public CompletionStage<Void> send(Mail... mails) {
        return client.send(mails).subscribeAsCompletionStage();
    }
}
