package io.quarkus.rest.client.reactive.error;

import static io.quarkus.rest.client.reactive.RestClientTestUtil.setUrlForClass;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.common.core.BlockingNotAllowedException;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.client.reactive.ClientExceptionMapper;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.common.annotation.Blocking;
import io.vertx.core.Context;

public class BlockingExceptionMapperTest {

    private static final AtomicBoolean EVENT_LOOP_THREAD_USED_BY_MAPPER = new AtomicBoolean();
    private static final int STATUS_FOR_BLOCKING_MAPPER = 501;
    private static final int STATUS_FOR_NON_BLOCKING_MAPPER = 500;

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Client.class,
                            ClientUsingNotBlockingExceptionMapper.class,
                            ClientUsingBlockingExceptionMapper.class,
                            ClientUsingBlockingExceptionMapperWithAnnotation.class,
                            ClientUsingBothExceptionMappers.class,
                            NotBlockingExceptionMapper.class,
                            BlockingExceptionMapper.class,
                            ClientResource.class,
                            Resource.class)
                    .addAsResource(
                            new StringAsset(setUrlForClass(ClientUsingNotBlockingExceptionMapper.class) + "\n"
                                    + setUrlForClass(ClientUsingBlockingExceptionMapper.class) + "\n"
                                    + setUrlForClass(ClientUsingBlockingExceptionMapperWithAnnotation.class) + "\n"
                                    + setUrlForClass(ClientUsingBothExceptionMappers.class) + "\n"),
                            "application.properties"));

    public static final String ERROR_MESSAGE = "The entity was not found";

    @RestClient
    ClientUsingNotBlockingExceptionMapper clientUsingNotBlockingExceptionMapper;

    @RestClient
    ClientUsingBlockingExceptionMapper clientUsingBlockingExceptionMapper;

    @RestClient
    ClientUsingBlockingExceptionMapperWithAnnotation clientUsingBlockingExceptionMapperWithAnnotation;

    @RestClient
    ClientUsingBothExceptionMappers clientUsingBothExceptionMappers;

    @BeforeEach
    public void setup() {
        EVENT_LOOP_THREAD_USED_BY_MAPPER.set(false);
    }

    @Test
    public void shouldUseEventLoopByDefault() {
        assertThrows(BlockingNotAllowedException.class, clientUsingNotBlockingExceptionMapper::nonBlocking);
        assertThat(EVENT_LOOP_THREAD_USED_BY_MAPPER.get()).isTrue();
    }

    @Test
    public void shouldUseWorkerThreadIfExceptionMapperIsAnnotatedWithBlocking() {
        RuntimeException exception = assertThrows(RuntimeException.class, clientUsingBlockingExceptionMapper::blocking);
        assertThat(EVENT_LOOP_THREAD_USED_BY_MAPPER.get()).isFalse();
        assertThat(exception.getMessage()).isEqualTo(ERROR_MESSAGE);
    }

    @Test
    public void shouldUseWorkerThreadOnlyIfExceptionMapperIsAnnotatedWithBlockingIsUsed() {
        assertThrows(BlockingNotAllowedException.class, clientUsingBothExceptionMappers::nonBlocking);
        assertThat(EVENT_LOOP_THREAD_USED_BY_MAPPER.get()).isTrue();

        RuntimeException exception = assertThrows(RuntimeException.class, clientUsingBothExceptionMappers::blocking);
        assertThat(EVENT_LOOP_THREAD_USED_BY_MAPPER.get()).isFalse();
        assertThat(exception.getMessage()).isEqualTo(ERROR_MESSAGE);
    }

    @Test
    public void shouldUseWorkerThreadWhenClientIsInjected() {
        given().get("/client/non-blocking").then().statusCode(500);
        assertThat(EVENT_LOOP_THREAD_USED_BY_MAPPER.get()).isTrue();

        given().get("/client/blocking").then().statusCode(500);
        assertThat(EVENT_LOOP_THREAD_USED_BY_MAPPER.get()).isFalse();
    }

    @Test
    public void shouldUseWorkerThreadIfExceptionMapperIsAnnotatedWithBlockingAndUsingClientExceptionMapper() {
        RuntimeException exception = assertThrows(RuntimeException.class,
                clientUsingBlockingExceptionMapperWithAnnotation::blocking);
        assertThat(EVENT_LOOP_THREAD_USED_BY_MAPPER.get()).isFalse();
        assertThat(exception.getMessage()).isEqualTo(ERROR_MESSAGE);
    }

    @Test
    public void shouldUseWorkerThreadUsingProgrammaticApproach() {
        var client = RestClientBuilder.newBuilder()
                .baseUri(UriBuilder.fromUri("http://localhost:8081").build())
                .register(BlockingExceptionMapper.class)
                .build(Client.class);

        RuntimeException exception = assertThrows(RuntimeException.class, client::blocking);
        assertThat(EVENT_LOOP_THREAD_USED_BY_MAPPER.get()).isFalse();
        assertThat(exception.getMessage()).isEqualTo(ERROR_MESSAGE);
    }

    @Path("/error")
    @RegisterRestClient
    public interface Client {
        @GET
        @Path("/blocking")
        InputStream blocking();
    }

    @Path("/error")
    @RegisterRestClient
    @RegisterProvider(NotBlockingExceptionMapper.class)
    public interface ClientUsingNotBlockingExceptionMapper {

        @GET
        @Path("/non-blocking")
        InputStream nonBlocking();
    }

    @Path("/error")
    @RegisterRestClient
    @RegisterProvider(BlockingExceptionMapper.class)
    public interface ClientUsingBlockingExceptionMapper {
        @GET
        @Path("/blocking")
        InputStream blocking();
    }

    @Path("/error")
    @RegisterRestClient
    @RegisterProvider(NotBlockingExceptionMapper.class)
    @RegisterProvider(BlockingExceptionMapper.class)
    public interface ClientUsingBothExceptionMappers {
        @GET
        @Path("/blocking")
        InputStream blocking();

        @GET
        @Path("/non-blocking")
        InputStream nonBlocking();
    }

    @Path("/error")
    @RegisterRestClient
    public interface ClientUsingBlockingExceptionMapperWithAnnotation {
        @GET
        @Path("/blocking")
        InputStream blocking();

        @Blocking
        @ClientExceptionMapper
        static RuntimeException map(Response response) {
            EVENT_LOOP_THREAD_USED_BY_MAPPER.set(Context.isOnEventLoopThread());
            return new RuntimeException(response.readEntity(String.class));
        }
    }

    public static class NotBlockingExceptionMapper implements ResponseExceptionMapper<Exception> {

        @Override
        public boolean handles(int status, MultivaluedMap<String, Object> headers) {
            return status == STATUS_FOR_NON_BLOCKING_MAPPER;
        }

        @Override
        public Exception toThrowable(Response response) {
            EVENT_LOOP_THREAD_USED_BY_MAPPER.set(Context.isOnEventLoopThread());
            // Reading InputStream in the Event Loop throws the BlockingNotAllowedException exception
            response.readEntity(String.class);
            return null;
        }
    }

    @Blocking
    public static class BlockingExceptionMapper implements ResponseExceptionMapper<Exception> {
        @Override
        public boolean handles(int status, MultivaluedMap<String, Object> headers) {
            return status == STATUS_FOR_BLOCKING_MAPPER;
        }

        @Override
        public Exception toThrowable(Response response) {
            EVENT_LOOP_THREAD_USED_BY_MAPPER.set(Context.isOnEventLoopThread());
            return new RuntimeException(response.readEntity(String.class));
        }
    }

    @Path("/error")
    public static class Resource {

        @GET
        @Path("/blocking")
        public Response blocking() {
            return Response.status(STATUS_FOR_BLOCKING_MAPPER).entity(ERROR_MESSAGE).build();
        }

        @GET
        @Path("/non-blocking")
        public Response nonBlocking() {
            return Response.status(STATUS_FOR_NON_BLOCKING_MAPPER).entity(ERROR_MESSAGE).build();
        }
    }

    @Path("/client")
    public static class ClientResource {

        @RestClient
        ClientUsingBothExceptionMappers clientUsingBothExceptionMappers;

        @GET
        @Path("/blocking")
        public void callBlocking() {
            clientUsingBothExceptionMappers.blocking();
        }

        @GET
        @Path("/non-blocking")
        public void callNonBlocking() {
            clientUsingBothExceptionMappers.nonBlocking();
        }
    }
}
