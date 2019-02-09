package io.vertx.axle.mail;

import io.vertx.axle.core.Vertx;
import io.vertx.axle.ext.mail.MailClient;
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.MailMessage;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class MailClientTest {

    @Rule
    public GenericContainer container = new GenericContainer("digiplant/fake-smtp")
            .withExposedPorts(25)
            .withFileSystemBind("target", "/tmp/fakemail", BindMode.READ_WRITE);

    private Vertx vertx;

    @Before
    public void setUp() {
        vertx = Vertx.vertx();
        assertThat(vertx, is(notNullValue()));
    }

    @After
    public void tearDown() {
        vertx.close();
    }

    @Test
    public void testAxleAPI() {
        MailClient client = MailClient.createShared(vertx, new MailConfig()
                .setPort(container.getMappedPort(25))
                .setHostname(container.getContainerIpAddress())
        );
        assertThat(client, is(notNullValue()));
        client.sendMail(new MailMessage().setText("hello Axle")
                .setSubject("test email")
                .setTo("clement@apache.org")
                .setFrom("clement@apache.org"))
                .toCompletableFuture().join();
    }
}
