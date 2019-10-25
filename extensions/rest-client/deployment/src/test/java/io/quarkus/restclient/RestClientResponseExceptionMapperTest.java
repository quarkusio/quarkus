package io.quarkus.restclient;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class RestClientResponseExceptionMapperTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(RestClientResponseExceptionMapperTest.class, Client.class, TestEndpoint.class,
                            MyExceptionMapper.class));

    @Inject
    @RestClient
    Client client;

    @Test
    public void testExceptionMapperIsCalled() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(client::test);
    }

    @RegisterRestClient(baseUri = "http://localhost:8081")
    @RegisterProvider(MyExceptionMapper.class)
    public interface Client {

        @GET
        @Path("/test")
        String test();

    }

    @Path("/test")
    public static class TestEndpoint {
        @GET
        Response get() {
            return Response.status(Response.Status.PAYMENT_REQUIRED).build();
        }
    }

    public static class MyExceptionMapper implements ResponseExceptionMapper<IllegalArgumentException> {
        @Override
        public IllegalArgumentException toThrowable(Response response) {
            return new IllegalArgumentException();
        }
    }
}
