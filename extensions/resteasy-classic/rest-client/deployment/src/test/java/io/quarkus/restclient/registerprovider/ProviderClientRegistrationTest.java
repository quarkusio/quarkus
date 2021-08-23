package io.quarkus.restclient.registerprovider;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.InetSocketAddress;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Priorities;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.ext.Provider;

import org.jboss.resteasy.specimpl.ResteasyUriInfo;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.sun.net.httpserver.HttpServer;

import io.quarkus.test.QuarkusUnitTest;

public class ProviderClientRegistrationTest {
    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(ProviderClientRegistrationTest.class, ClientBean.class, HelloFooProvider.class));

    static HttpServer server;

    @Inject
    ClientBean client;

    @BeforeAll
    static void beforeAll() throws Exception {
        // This Server is required, so we don't start a REST Application and test that we are able to add Providers
        // without a class annotated with @Path, to a Client: https://github.com/quarkusio/quarkus/issues/11424
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/hello", exchange -> {
            ResteasyUriInfo uriInfo = new ResteasyUriInfo(exchange.getRequestURI());
            String who = uriInfo.getQueryParameters().getFirst("name");
            String response = "Hello ";
            if (who == null) {
                response = response + "Undefined";
            } else {
                response = response + who;
            }

            exchange.sendResponseHeaders(200, response.length());
            exchange.getResponseHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN);
            exchange.getResponseBody().write(response.getBytes());
            exchange.getResponseBody().flush();
            exchange.getResponseBody().close();
        });

        server.start();
    }

    @AfterAll
    static void afterAll() {
        server.stop(0);
    }

    @Test
    void hello() {
        assertEquals("Hello Naruto", client.hello("Naruto"));
    }

    @Test
    void helloFoo() {
        assertEquals("Hello Foo", client.hello());
    }

    // The Client needs to be deployed with the Quarkus app, so it inherits all of the RESTEasy configuration.
    @ApplicationScoped
    public static class ClientBean {
        private WebTarget webTarget;
        private Client client;

        @PostConstruct
        void init() {
            client = ClientBuilder.newClient();
            webTarget = client.target("http://localhost:" + server.getAddress().getPort()).path("/hello");
        }

        @PreDestroy
        void close() {
            client.close();
        }

        String hello() {
            return hello(null);
        }

        String hello(final String name) {
            WebTarget webTarget = this.webTarget;
            if (name != null) {
                webTarget = webTarget.queryParam("name", name);
            }

            Response response = webTarget.request(MediaType.TEXT_PLAIN_TYPE).get(Response.class);

            assertEquals(200, response.getStatus());
            return response.readEntity(String.class);
        }
    }

    @Provider
    @Priority(Priorities.USER - 1)
    public static class HelloFooProvider implements ClientRequestFilter {
        @Override
        public void filter(final ClientRequestContext requestContext) {
            if (requestContext.getUri().getQuery() == null) {
                requestContext.setUri(UriBuilder.fromUri(requestContext.getUri()).queryParam("name", "Foo").build());
            }
        }
    }
}
