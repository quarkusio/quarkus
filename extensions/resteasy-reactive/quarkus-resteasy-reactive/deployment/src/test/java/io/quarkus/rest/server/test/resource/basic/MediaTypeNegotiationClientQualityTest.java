package io.quarkus.rest.server.test.resource.basic;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.function.Supplier;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.server.test.simple.PortProviderUtil;
import io.quarkus.test.QuarkusUnitTest;

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
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
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
