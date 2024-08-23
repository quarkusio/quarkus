package io.quarkus.rest.client.reactive;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Provider;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.reactive.client.spi.ResteasyReactiveClientRequestContext;
import org.jboss.resteasy.reactive.client.spi.ResteasyReactiveClientRequestFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class ProvidersFromContextTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest();

    @TestHTTPResource
    URI baseUri;

    private Client client;

    @BeforeEach
    public void before() {
        client = QuarkusRestClientBuilder.newBuilder()
                .baseUri(baseUri)
                .register(TestClientRequestFilter.class)
                .register(MyContextResolver.class)
                .build(Client.class);
    }

    @Test
    public void test() {
        Response response = client.get();
        assertEquals(200, response.getStatus());
    }

    @RegisterRestClient
    public interface Client {

        @GET
        @Path("test")
        Response get();
    }

    @Path("test")
    public static class Endpoint {

        @GET
        public Response get() {
            return Response.ok().build();
        }
    }

    public static class Person {
        public String name;
    }

    public static class MyContextResolver implements ContextResolver<Person> {

        @Override
        public Person getContext(Class<?> aClass) {
            return new Person();
        }
    }

    @Provider
    public static class TestClientRequestFilter implements ResteasyReactiveClientRequestFilter {

        @Override
        public void filter(ResteasyReactiveClientRequestContext requestContext) {
            if (requestContext.getProviders() == null) {
                throw new RuntimeException("Providers was not injected");
            }

            var readers = requestContext.getProviders().getMessageBodyReader(String.class, null, null, null);
            if (readers == null) {
                throw new RuntimeException("No readers were found");
            }

            var writers = requestContext.getProviders().getMessageBodyWriter(String.class, null, null, null);
            if (writers == null) {
                throw new RuntimeException("No writers were found");
            }

            ContextResolver<Person> contextResolver = requestContext.getProviders().getContextResolver(Person.class, null);
            if (contextResolver == null) {
                throw new RuntimeException("Context resolver was not found");
            }
        }
    }
}
