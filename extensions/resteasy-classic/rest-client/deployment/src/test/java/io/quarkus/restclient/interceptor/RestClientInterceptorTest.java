package io.quarkus.restclient.interceptor;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.net.URL;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InterceptorBinding;
import javax.interceptor.InvocationContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class RestClientInterceptorTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(RestClientInterceptorTest.class, Client.class, Foo.class, FooInterceptor.class));

    @TestHTTPResource
    URL url;

    @Test
    public void testInterception() {
        Client client = RestClientBuilder.newBuilder().baseUrl(url).build(Client.class);
        assertEquals("foo", client.ping());
    }

    @RegisterRestClient
    public interface Client {

        @Foo
        @GET
        @Path("/test")
        String ping();

    }

    @Path("/test")
    public static class TestEndpoint {

        @GET
        public String get() {
            throw new WebApplicationException(404);
        }

    }

    @Target({ TYPE, METHOD })
    @Retention(RUNTIME)
    @Documented
    @InterceptorBinding
    public @interface Foo {

    }

    @Foo
    @Priority(1)
    @Interceptor
    public static class FooInterceptor {

        @AroundInvoke
        Object aroundInvoke(InvocationContext ctx) {
            return "foo";
        }

    }
}
