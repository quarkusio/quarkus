package io.quarkus.rest.client.reactive.multipart;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.jboss.resteasy.reactive.MultipartForm;
import org.jboss.resteasy.reactive.PartType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class MultipartCleanupTest {

    private static java.nio.file.Path tempDir;

    @TestHTTPResource
    URI baseUri;

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(Form.class, Client.class, Resource.class));

    @BeforeAll
    static void getTempDir() throws IOException {
        java.nio.file.Path tempFilePath = Files.createTempFile("jvm_tempfile_helper_", ".tmp"); // create temp file to get reference to jvm temp dir
        tempFilePath.toFile().deleteOnExit(); // cleanup file after test is finished
        tempDir = tempFilePath.getParent(); // now we can get the root temp dir
    }

    @Test
    public void test() throws IOException {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri).build(Client.class);

        File file = File.createTempFile("MultipartTest", ".txt");
        Files.writeString(file.toPath(), "DUMMY".repeat(1000));
        file.deleteOnExit();

        Form form = new Form();
        form.file = file;
        form.dummy = "dummy";

        long attrFilesBeforeTest = getCountOfNettyAttrTempFiles();
        assertThat(client.send(form)).isEqualTo("test");
        assertThat(getCountOfNettyAttrTempFiles()).isEqualTo(attrFilesBeforeTest);
    }

    private long getCountOfNettyAttrTempFiles() {
        File[] attrFiles = tempDir.toFile().listFiles((dir, name) -> name.startsWith("Attr_"));
        if (attrFiles == null) {
            throw new IllegalStateException("could not list Attr_ files because the directory to search was not valid");
        }
        return attrFiles.length;
    }

    public static class Form {
        @FormParam("file")
        @PartType(MediaType.APPLICATION_OCTET_STREAM)
        private File file;

        @FormParam("dummy")
        @PartType(MediaType.TEXT_PLAIN)
        private String dummy;
    }

    public interface Client {

        @POST
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        @Path("/multipart")
        String send(Form collectorDto);
    }

    @Path("/multipart")
    @ApplicationScoped
    public static class Resource {
        @POST
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        public String upload(@MultipartForm Form form) {
            return "test";
        }
    }
}
