package io.vertx.axle.mail.test;

import io.vertx.axle.core.Vertx;
import io.vertx.axle.ext.mail.MailClient;
import io.vertx.ext.mail.MailConfig;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class APITest {


    @Test
    public void testAxleAPI() {
        Vertx vertx = Vertx.vertx();
        assertThat(vertx, is(notNullValue()));
        MailClient client = MailClient.createShared(vertx, new MailConfig().setPort(25));
        assertThat(client, is(notNullValue()));
    }
}
