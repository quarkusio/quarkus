package io.quarkus.mailer.runtime;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import io.vertx.ext.mail.MailClient;

/**
 * Beans producing the Vert.x Mail clients.
 */
@ApplicationScoped
public class MailClientProducer {

    private volatile io.vertx.axle.ext.mail.MailClient axleMailClient;
    private volatile io.vertx.reactivex.ext.mail.MailClient rxMailClient;
    private volatile MailClient client;

    void initialize(MailClient client) {
        this.client = client;
        this.axleMailClient = io.vertx.axle.ext.mail.MailClient.newInstance(client);
        this.rxMailClient = io.vertx.reactivex.ext.mail.MailClient.newInstance(client);
    }

    @Singleton
    @Produces
    public MailClient mailClient() {
        return client;
    }

    @Singleton
    @Produces
    public io.vertx.axle.ext.mail.MailClient axleMailClient() {
        return axleMailClient;
    }

    @Singleton
    @Produces
    public io.vertx.reactivex.ext.mail.MailClient rxMailClient() {
        return rxMailClient;
    }

}
