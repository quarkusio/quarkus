package io.quarkus.rest.client.reactive.redirect;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.client.reactive.ClientExceptionMapper;
import io.quarkus.rest.client.reactive.ClientRedirectHandler;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class MultipleProvidersFromAnnotationTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Client.class, Resource.class));

    @Test
    void test() {
        Client client = RestClientBuilder.newBuilder()
                .baseUri(uri)
                .followRedirects(true)
                .build(Client.class);
        assertThatThrownBy(() -> client.call(2)).hasMessage("dummy");
    }

    @TestHTTPResource
    URI uri;

    @Path("test")
    public interface Client {

        @GET
        void call(@QueryParam("redirects") Integer numberOfRedirects);

        @ClientRedirectHandler
        static URI redirectFor3xx(Response response) {
            int status = response.getStatus();
            if (status > 300 && response.getStatus() < 400) {
                return response.getLocation();
            }

            return null;
        }

        @ClientExceptionMapper
        static RuntimeException toException(Response response) {
            if (response.getStatus() == 999) {
                throw new RuntimeException("dummy") {
                    @Override
                    public synchronized Throwable fillInStackTrace() {
                        return this;
                    }
                };
            }
            return null;
        }
    }

    @Path("test")
    public static class Resource {

        @GET
        public Response redirectedResponse(@QueryParam("redirects") Integer number) {
            if (number == null || 0 == number) {
                return Response.status(999).build();
            } else {
                return Response.status(Response.Status.FOUND).location(URI.create("/test?redirects=" + (number - 1)))
                        .build();
            }
        }
    }
}
