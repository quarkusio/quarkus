package io.quarkus.rest.client.reactive;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.jboss.resteasy.reactive.MultipartForm;
import org.jboss.resteasy.reactive.PartType;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class MultipartFilenameTest {

    @TestHTTPResource
    URI baseUri;

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Resource.class, FormData.class, Client.class, ClientForm.class))
            .withConfigurationResource("mp-global-scope-test-application.properties");

    @Test
    void shouldPassOriginalFileName() throws IOException {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri).build(Client.class);

        File file = File.createTempFile("MultipartTest", ".txt");
        file.deleteOnExit();

        ClientForm form = new ClientForm();
        form.file = file;
        assertThat(client.postMultipart(form)).isEqualTo(file.getName());
    }

    @Path("/multipart")
    @ApplicationScoped
    public static class Resource {
        @POST
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        public String upload(@MultipartForm FormData form) {
            return form.myFile.fileName();
        }
    }

    public static class FormData {
        @FormParam("myFile")
        public FileUpload myFile;

    }

    @Path("/multipart")
    public interface Client {
        @POST
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        String postMultipart(@MultipartForm ClientForm clientForm);

    }

    public static class ClientForm {
        @FormParam("myFile")
        @PartType(MediaType.APPLICATION_OCTET_STREAM)
        public File file;
    }
}
