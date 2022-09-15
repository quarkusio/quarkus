package org.jboss.resteasy.reactive.server.vertx.test.resource.basic;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.function.Supplier;
import org.jboss.resteasy.reactive.server.vertx.test.framework.ResteasyReactiveUnitTest;
import org.jboss.resteasy.reactive.server.vertx.test.simple.PortProviderUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@DisplayName("Media Type Negotiation Client Quality Test")
public class MediaTypeNegotiationClientQualityTest {

    @Produces({ "application/x;qs=0.9", "application/y;qs=0.7" })
    @DisplayName("Custom Message Body Writer 1")
    @Provider
    public static class CustomMessageBodyWriter1 implements MessageBodyWriter<Object> {

        @Override
        public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return true;
        }

        @Override
        public long getSize(Object t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return -1;
        }

        @Override
        public void writeTo(Object t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
                MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
                throws IOException, WebApplicationException {
        }
    }

    @Path("/echo")
    public static class Resource {
        @GET
        public Object nothing() {
            return new Object();
        }
    }

    private static Client client;

    private static final String DEP = "MediaTypeNegotiationClientQualityTest";

    @RegisterExtension
    static ResteasyReactiveUnitTest testExtension = new ResteasyReactiveUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class, CustomMessageBodyWriter1.class, Resource.class);
                    return war;
                }
            });

    @BeforeAll
    public static void setup() {
        client = ClientBuilder.newClient();
    }

    @AfterAll
    public static void cleanup() {
        client.close();
    }

    private String generateURL() {
        return PortProviderUtil.generateBaseUrl();
    }

    @Test
    @DisplayName("Test Client Quality")
    public void testClientQuality() throws Exception {
        Invocation.Builder request = client.target(generateURL()).path("echo").request("application/x;q=0.7",
                "application/y;q=0.9");
        Response response = request.get();
        try {
            Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
            MediaType mediaType = response.getMediaType();
            Assertions.assertEquals(mediaType.getType(), "application");
            Assertions.assertEquals(mediaType.getSubtype(), "y");
        } finally {
            response.close();
        }
    }
}
