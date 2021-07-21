package io.quarkus.mailer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import io.vertx.mutiny.core.Vertx;

public class FakeSmtpTestBase {

    protected static final String FROM = "test@test.org";
    protected static final String TO = "foo@quarkus.io";

    protected Vertx vertx;
    protected FakeSmtpServer smtpServer;

    @BeforeEach
    void startVertx() {
        vertx = Vertx.vertx();
    }

    @AfterEach
    void stopVertx() {
        vertx.close().await().indefinitely();
    }

    protected void startServer(String keystore) {
        smtpServer = new FakeSmtpServer(vertx, keystore != null, keystore);
    }

    protected Mail getMail() {
        return new Mail().setFrom(FROM).addTo(TO).setSubject("Subject").setText("Message");
    }
}
