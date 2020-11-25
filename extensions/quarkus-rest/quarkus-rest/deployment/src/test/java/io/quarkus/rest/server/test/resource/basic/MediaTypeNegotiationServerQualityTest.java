package io.quarkus.rest.server.test.resource.basic;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.function.Supplier;

import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
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
import javax.ws.rs.ext.ExceptionMapper;
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

@DisplayName("Media Type Negotiation Server Quality Test")
public class MediaTypeNegotiationServerQualityTest {

    @Produces({ "application/*;qs=0.7", "text/*;qs=0.9" })
    @DisplayName("Custom Message Body Writter")
    @Provider
    public static class CustomMessageBodyWriter implements MessageBodyWriter<Object> {

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

    @DisplayName("Not Found Exception Mapper")
    @Provider
    public static class NotFoundExceptionMapper implements ExceptionMapper<NotFoundException> {

        @Override
        public Response toResponse(NotFoundException notFoundException) {
            return Response.status(Status.NOT_FOUND).entity(new Object()).build();
        }
    }

    @Path("/foo")
    public static class FakeResource {
        @GET
        @Path("/fake")
        public String fake() {
            return "";
        }
    }

    private static Client client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class, CustomMessageBodyWriter.class, FakeResource.class,
                            NotFoundExceptionMapper.class);
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
    @DisplayName("Test Server Quality")
    public void testServerQuality() throws Exception {
        Invocation.Builder request = client.target(generateURL()).path("foo/echo").request("application/x;", "text/y");
        Response response = request.get();
        try {
            Assertions.assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
            MediaType mediaType = response.getMediaType();
            Assertions.assertEquals("text", mediaType.getType());
            Assertions.assertEquals("y", mediaType.getSubtype());
        } finally {
            response.close();
        }
    }
}
