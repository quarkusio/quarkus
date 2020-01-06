package io.quarkus.mailer.runtime;

import java.util.concurrent.CompletionStage;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.axle.ReactiveMailer;

@ApplicationScoped
public class AxleMailerImpl implements ReactiveMailer, io.quarkus.mailer.ReactiveMailer {

    @Inject
    io.quarkus.mailer.mutiny.ReactiveMailer client;

    @Override
    public CompletionStage<Void> send(Mail... mails) {
        return client.send(mails).subscribeAsCompletionStage();
    }
}
