package io.quarkus.rest.client.reactive.multipart;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.ResponseStatus;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.client.api.QuarkusRestClientProperties;
import org.jboss.resteasy.reactive.client.impl.multipart.PausableHttpPostRequestEncoder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class MultipartEncoderModeTest {
    @TestHTTPResource
    URI baseUri;

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(Resource.class, Client.class))
            .withConfigurationResource("multipart-encoder-mode-test.properties");

    @Inject
    @RestClient
    Client client;

    @Test
    void shouldPassUsingCustomMultipartPostEncoderMode(@TempDir File tempDir) throws IOException {
        File file = File.createTempFile("MultipartTest", ".txt", tempDir);
        assertThat(client.upload(file)).isEqualTo("OK");
    }

    /**
     * This filter is present to check in advance if property {@link QuarkusRestClientProperties#MULTIPART_ENCODER_MODE}
     * is of the right type.
     */
    static class MultipartEncoderModeCheck implements ClientRequestFilter {
        @Override
        public void filter(ClientRequestContext requestContext) throws IOException {
            Object mode = requestContext.getConfiguration().getProperty(QuarkusRestClientProperties.MULTIPART_ENCODER_MODE);
            if (mode == null) {
                requestContext.abortWith(Response.serverError().entity("encoderMode is null").build());
                return;
            }
            if (mode.getClass() != PausableHttpPostRequestEncoder.EncoderMode.class) {
                requestContext.abortWith(Response.serverError().entity("encoderMode illegal type").build());
                return;
            }
        }
    }

    @Path("resource")
    static public class Resource {
        @Path("upload")
        @POST
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        @Produces(MediaType.TEXT_PLAIN)
        @ResponseStatus(200)
        public String upload(@RestForm File file) {
            return "OK";
        }
    }

    @Path("resource")
    @RegisterRestClient(configKey = "client")
    @RegisterProvider(MultipartEncoderModeCheck.class)
    static public interface Client {
        @Path("upload")
        @POST
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        @Produces(MediaType.TEXT_PLAIN)
        String upload(@FormParam("file") File file);
    }
}
