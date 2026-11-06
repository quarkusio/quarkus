package io.quarkus.micrometer.deployment.binder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.is;

import java.time.Duration;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

/**
 * The HTTP server and REST client timers are cached: check they are recorded like without a cache, including after the
 * meters are removed from the registry, as the dev mode restart does.
 */
public class HttpTimerCacheTest {

    private static final String SERVER = "http.server.requests";
    private static final String CLIENT = "http.client.requests";

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(Resource.class, Client.class))
            .overrideConfigKey("quarkus.otel.enabled", "false")
            .overrideConfigKey("quarkus.redis.devservices.enabled", "false")
            .overrideConfigKey("quarkus.micrometer.binder-enabled-default", "false")
            .overrideConfigKey("quarkus.micrometer.binder.http-client.enabled", "true")
            .overrideConfigKey("quarkus.micrometer.binder.http-server.enabled", "true")
            .overrideConfigKey("quarkus.micrometer.binder.vertx.enabled", "true")
            .overrideConfigKey("quarkus.rest-client.cached.url", "${test.url}");

    @Inject
    MeterRegistry registry;

    @Test
    public void timersAreRecordedBeforeAndAfterTheMetersAreRemoved() {
        for (int i = 0; i < 3; i++) {
            RestAssured.get("/cached/call/" + i).then().statusCode(200).body(is("" + i));
        }
        awaitServerCount("/cached/{id}", 3);
        awaitServerCount("/cached/call/{id}", 3);
        assertThat(clientTimer().count()).isEqualTo(3);
        assertThat(registry.find(SERVER).tag("uri", "/cached/{id}").timers()).hasSize(1);
        assertThat(registry.find(CLIENT).timers()).hasSize(1);

        // what the dev mode restart does with the meters
        for (Meter meter : Metrics.globalRegistry.getMeters()) {
            Metrics.globalRegistry.remove(meter);
        }
        assertThat(Metrics.globalRegistry.find(SERVER).timers()).isEmpty();
        assertThat(Metrics.globalRegistry.find(CLIENT).timers()).isEmpty();

        RestAssured.get("/cached/call/42").then().statusCode(200);

        // new timers are registered and recorded to: a cached removed timer would not be registered anymore
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            Timer server = Metrics.globalRegistry.find(SERVER).tag("uri", "/cached/{id}").timer();
            assertThat(server).isNotNull();
            assertThat(server.count()).isEqualTo(1);
        });
        Timer client = Metrics.globalRegistry.find(CLIENT).tag("uri", "/cached/{id}").timer();
        assertThat(client).isNotNull();
        assertThat(client.count()).isEqualTo(1);
        assertThat(client.getId().getTag("status")).isEqualTo("200");
        assertThat(client.getId().getTag("method")).isEqualTo("GET");
        assertThat(client.getId().getTag("outcome")).isEqualTo("SUCCESS");
    }

    private Timer clientTimer() {
        return registry.find(CLIENT).tag("uri", "/cached/{id}").timer();
    }

    private void awaitServerCount(String uri, long count) {
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            Timer timer = registry.find(SERVER).tag("uri", uri).timer();
            assertThat(timer).isNotNull();
            assertThat(timer.count()).isEqualTo(count);
        });
    }

    @Path("/cached")
    @RegisterRestClient(configKey = "cached")
    public interface Client {
        @GET
        @Path("/{id}")
        String get(@PathParam("id") String id);
    }

    @Path("/cached")
    @Singleton
    public static class Resource {

        @RestClient
        Client client;

        @GET
        @Path("/{id}")
        public String get(@PathParam("id") String id) {
            return id;
        }

        @GET
        @Path("/call/{id}")
        public String call(@PathParam("id") String id) {
            return client.get(id);
        }
    }
}
