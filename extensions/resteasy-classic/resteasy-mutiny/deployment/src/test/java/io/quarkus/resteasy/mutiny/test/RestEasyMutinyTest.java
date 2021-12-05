package io.quarkus.resteasy.mutiny.test;

import java.net.URL;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.resteasy.mutiny.test.annotations.Async;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class RestEasyMutinyTest {

    private static AtomicReference<Object> value = new AtomicReference<>();

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MutinyResource.class, MutinyInjector.class, Async.class));

    @TestHTTPResource
    URL url;

    private ResteasyClient client;

    @BeforeEach
    public void before() {
        client = ((ResteasyClientBuilder) ClientBuilder.newBuilder())
                .readTimeout(5, TimeUnit.SECONDS)
                .connectionCheckoutTimeout(5, TimeUnit.SECONDS)
                .connectTimeout(5, TimeUnit.SECONDS)
                .build();
        value.set(null);
    }

    @AfterEach
    public void after() {
        client.close();
    }

    @Test
    public void testInjection() {
        Integer data = client.target(url.toExternalForm() + "/injection").request().get(Integer.class);
        Assertions.assertEquals((Integer) 42, data);

        data = client.target(url.toExternalForm() + "/injection-async").request().get(Integer.class);
        Assertions.assertEquals((Integer) 42, data);
    }

    @Test
    public void testFailingWithWebApplicationException() {
        Response response = client.target(url.toExternalForm() + "/web-failure").request().get();
        Assertions.assertEquals(response.getStatus(), Response.Status.SERVICE_UNAVAILABLE.getStatusCode());
    }

    @Test
    public void testFailingWithApplicationFailure() {
        Response response = client.target(url.toExternalForm() + "/app-failure").request().get();
        Assertions.assertEquals(response.getStatus(), 500);
    }

    @Test
    public void testHttpResponseTeaPot() {
        Response response = client.target(url.toExternalForm() + "/response/tea-pot").request().get();
        Assertions.assertEquals(418, response.getStatus());
    }

    @Test
    public void testHttpResponseNoContent() {
        Response response = client.target(url.toExternalForm() + "/response/no-content").request().get();
        Assertions.assertEquals(204, response.getStatus());
    }

    @Test
    public void testHttpResponseAcceptedWithBody() {
        Response response = client.target(url.toExternalForm() + "/response/accepted").request().get();
        Assertions.assertEquals(202, response.getStatus());
        Assertions.assertEquals("Hello", response.readEntity(String.class));
    }

    @Test
    public void testHttpResponseConditional() {
        Response response = client.target(url.toExternalForm() + "/response/conditional/true").request().get();
        Assertions.assertEquals(202, response.getStatus());
        response = client.target(url.toExternalForm() + "/response/conditional/false").request().get();
        Assertions.assertEquals(204, response.getStatus());
    }

}
