package io.quarkus.rest.client.reactive.error;

import static io.quarkus.rest.client.reactive.RestClientTestUtil.setUrlForClass;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Uni;

public class ResponseExceptionMapperTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Client.class, Resource.class, MyExceptionMapper.class).addAsResource(
                            new StringAsset(setUrlForClass(Client.class)),
                            "application.properties"));
    public static final String ERROR_MESSAGE = "The entity was not found";

    @RestClient
    Client client;

    @Test
    void shouldInvokeExceptionMapperOnce() {
        assertThrows(TestException.class, client::get);
        assertThat(MyExceptionMapper.executionCount.get()).isEqualTo(1);

        assertThrows(TestException.class, () -> client.uniGet().await().indefinitely());
        assertThat(MyExceptionMapper.executionCount.get()).isEqualTo(2);
    }

    @Path("/error")
    public static class Resource {
        @GET
        public Response returnError() {
            return Response.status(404).entity(ERROR_MESSAGE).build();
        }
    }

    @Path("/error")
    @RegisterRestClient
    public interface Client {
        @GET
        String get();

        @GET
        Uni<String> uniGet();
    }

    @Provider
    public static class MyExceptionMapper implements ResponseExceptionMapper<Exception> {

        private static final AtomicInteger executionCount = new AtomicInteger();

        @Override
        public Exception toThrowable(Response response) {
            executionCount.incrementAndGet();
            return new TestException("My exception");
        }
    }

    public static class TestException extends RuntimeException {

        public TestException(String message) {
            super(message);
        }
    }

}
