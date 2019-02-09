package io.vertx.axle.web;

import io.vertx.axle.core.Vertx;
import io.vertx.axle.ext.web.client.HttpResponse;
import io.vertx.axle.ext.web.client.WebClient;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClientOptions;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class WebClientTest {

    @Rule
    public GenericContainer container = new GenericContainer("kennethreitz/httpbin")
            .withExposedPorts(80)
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
    public void testWebClient() {
        WebClient client = WebClient.create(vertx, new WebClientOptions()
                .setDefaultPort(container.getMappedPort(80))
                .setDefaultHost(container.getContainerIpAddress())
        );
        assertThat(client, is(notNullValue()));

        JsonObject object = client.get("/get?msg=hello").send()
                .thenApply(HttpResponse::bodyAsJsonObject)
                .toCompletableFuture().join();
        assertThat(object.getJsonObject("args").getString("msg"), is("hello"));
    }
}
