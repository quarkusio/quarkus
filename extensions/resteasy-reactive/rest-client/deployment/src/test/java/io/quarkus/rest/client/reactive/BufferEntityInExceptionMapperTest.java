package io.quarkus.rest.client.reactive;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;
import org.jboss.resteasy.reactive.RestResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.smallrye.mutiny.Uni;
import io.vertx.core.impl.NoStackTraceException;

public class BufferEntityInExceptionMapperTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(
                    Client.class, Resource.class, DummyExceptionMapper.class));

    @TestHTTPResource
    URI uri;

    @Test
    public void testBlocking() {
        Client client = createClient();

        assertThatThrownBy(client::hello).isInstanceOfSatisfying(NoStackTraceException.class,
                e -> assertThat(e).hasMessage("dummy"));
    }

    @Test
    @RunOnVertxContext
    public void testEventLoop(UniAsserter asserter) {
        Client client = createClient();

        asserter.assertThat(
                () -> client.uniHello().onFailure(NoStackTraceException.class).recoverWithItem(Throwable::getMessage),
                (res) -> {
                    assertThat(res).isEqualTo("dummy");
                });
    }

    private Client createClient() {
        return RestClientBuilder.newBuilder()
                .baseUri(uri)
                .build(Client.class);
    }

    @Path("resource")
    @RegisterProvider(DummyExceptionMapper.class)
    public interface Client {

        @Path("/hello")
        @GET
        Uni<String> uniHello();

        @Path("/hello")
        @GET
        String hello();
    }

    @Path("resource")
    public static class Resource {

        @GET
        @Path("/hello")
        @Produces(MediaType.TEXT_PLAIN)
        public RestResponse<Object> hello() {
            return RestResponse.ResponseBuilder.serverError().build();
        }
    }

    @Provider
    public static class DummyExceptionMapper implements ResponseExceptionMapper<RuntimeException> {

        @Override
        public RuntimeException toThrowable(Response response) {
            response.bufferEntity();
            return new NoStackTraceException("dummy");
        }

    }
}
