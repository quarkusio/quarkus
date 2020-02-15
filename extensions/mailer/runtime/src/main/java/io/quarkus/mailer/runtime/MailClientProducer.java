package io.quarkus.mailer.runtime;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import org.jboss.logging.Logger;

import io.vertx.ext.mail.MailClient;

/**
 * Beans producing the Vert.x Mail clients.
 */
@ApplicationScoped
public class MailClientProducer {

    private static final Logger LOGGER = Logger.getLogger(MailClientProducer.class);

    private io.vertx.axle.ext.mail.MailClient axleMailClient;
    private io.vertx.reactivex.ext.mail.MailClient rxMailClient;
    private io.vertx.mutiny.ext.mail.MailClient mutinyClient;
    private MailClient client;

    synchronized void initialize(MailClient client) {
        this.client = client;
        this.mutinyClient = io.vertx.mutiny.ext.mail.MailClient.newInstance(client);
    }

    @Singleton
    @Produces
    public synchronized MailClient mailClient() {
        return client;
    }

    @Singleton
    @Produces
    public synchronized io.vertx.mutiny.ext.mail.MailClient mutinyClient() {
        return mutinyClient;
    }

    @Singleton
    @Produces
    @Deprecated
    public synchronized io.vertx.axle.ext.mail.MailClient axleMailClient() {
        if (axleMailClient == null) {
            LOGGER.warn(
                    "`io.vertx.axle.ext.mail.MailClient` is deprecated and will be removed in a future version - it is "
                            + "recommended to switch to `io.vertx.mutiny.ext.mail.MailClient`");
            axleMailClient = io.vertx.axle.ext.mail.MailClient.newInstance(client);
        }
        return axleMailClient;
    }

    @Singleton
    @Produces
    @Deprecated
    public synchronized io.vertx.reactivex.ext.mail.MailClient rxMailClient() {
        if (rxMailClient == null) {
            LOGGER.warn(
                    "`io.vertx.reactivex.ext.mail.MailClient` is deprecated and will be removed in a future version - it is "
                            + "recommended to switch to `io.vertx.mutiny.ext.mail.MailClient`");
            rxMailClient = io.vertx.reactivex.ext.mail.MailClient.newInstance(client);
        }
        return rxMailClient;
    }

}
