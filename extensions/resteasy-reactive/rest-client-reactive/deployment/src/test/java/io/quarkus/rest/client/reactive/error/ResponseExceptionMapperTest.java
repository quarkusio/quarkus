package io.quarkus.rest.client.reactive.error;

import static io.quarkus.rest.client.reactive.RestClientTestUtil.setUrlForClass;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.atomic.AtomicInteger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class ResponseExceptionMapperTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(Client.class, Resource.class, MyExceptionMapper.class).addAsResource(
                            new StringAsset(setUrlForClass(Client.class)),
                            "application.properties"));
    public static final String ERROR_MESSAGE = "The entity was not found";

    @RestClient
    Client client;

    @Test
    void shouldInvokeExceptionMapperOnce() {
        assertThrows(RuntimeException.class, client::get);
        assertThat(MyExceptionMapper.executionCount.get()).isEqualTo(1);
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
    }

    @Provider
    public static class MyExceptionMapper implements ResponseExceptionMapper<Exception> {

        private static final AtomicInteger executionCount = new AtomicInteger();

        @Override
        public Exception toThrowable(Response response) {
            executionCount.incrementAndGet();
            return new RuntimeException("My exception");
        }
    }

}
