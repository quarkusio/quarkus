package io.quarkus.rest.client.reactive;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.common.vertx.VertxContext;
import io.vertx.core.Vertx;
import io.vertx.core.internal.ContextInternal;
import io.vertx.core.spi.context.storage.AccessMode;

/**
 * A duplicated context carries no data map until someone writes a context local to it, because
 * context locals start unset. Invoking the client from such a context must not fail.
 */
public class DuplicatedContextWithoutLocalsTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar.addClasses(Resource.class, Client.class))
            .overrideRuntimeConfigKey("quarkus.rest-client.client.url",
                    "http://localhost:${quarkus.http.test-port:8081}");

    @Inject
    Vertx vertx;

    @Inject
    @RestClient
    Client client;

    @Test
    void shouldInvokeClientFromDuplicatedContextWithoutLocals() throws Exception {
        ContextInternal duplicated = ((ContextInternal) vertx.getOrCreateContext()).duplicate();
        assertThat(duplicated.getLocal(VertxContext.DATA_MAP_LOCAL, AccessMode.CONCURRENT)).isNull();

        CompletableFuture<String> result = new CompletableFuture<>();
        // Dispatch the duplicated context onto a regular thread, the way a JCA resource adapter
        // delivers a message to a message endpoint.
        Thread thread = new Thread(() -> {
            duplicated.beginDispatch();
            try {
                result.complete(client.get());
            } catch (Throwable t) {
                result.completeExceptionally(t);
            } finally {
                duplicated.endDispatch(null);
            }
        });
        thread.start();
        thread.join();

        assertThat(result.get(10, TimeUnit.SECONDS)).isEqualTo("test");
    }

    @Path("test")
    public static class Resource {

        @Path("toClient")
        @GET
        public String toClient() {
            return "test";
        }
    }

    @Path("test")
    @RegisterRestClient(configKey = "client")
    public interface Client {

        @GET
        @Path("toClient")
        String get();
    }
}
