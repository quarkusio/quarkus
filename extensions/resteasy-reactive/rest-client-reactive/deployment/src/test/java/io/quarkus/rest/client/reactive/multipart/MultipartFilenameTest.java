package io.quarkus.rest.client.reactive.multipart;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.jboss.resteasy.reactive.MultipartForm;
import org.jboss.resteasy.reactive.PartFilename;
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
            .withApplicationRoot(jar -> jar.addClasses(Resource.class, FormData.class, Client.class, ClientForm.class));

    @Test
    void shouldPassOriginalFileName() throws IOException {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri).build(Client.class);

        File file = File.createTempFile("MultipartTest", ".txt");
        file.deleteOnExit();

        ClientForm form = new ClientForm();
        form.file = file;
        assertThat(client.postMultipart(form)).isEqualTo(file.getName());
    }

    @Test
    void shouldUseFileNameFromAnnotation() throws IOException {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri).build(Client.class);

        File file = File.createTempFile("MultipartTest", ".txt");
        file.deleteOnExit();

        ClientForm2 form = new ClientForm2();
        form.file = file;
        assertThat(client.postMultipartWithPartFilename(form)).isEqualTo(ClientForm2.FILE_NAME);
    }

    @Test
    void shouldCopyFileContentToString() throws IOException {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri).build(Client.class);

        File file = File.createTempFile("MultipartTest", ".txt");
        Files.writeString(file.toPath(), "content!");
        file.deleteOnExit();

        ClientForm form = new ClientForm();
        form.file = file;
        assertThat(client.postMultipartWithFileContent(form)).isEqualTo("content!");
    }

    @Test
    void shouldCopyFileContentToBytes() throws IOException {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri).build(Client.class);

        File file = File.createTempFile("MultipartTest", ".txt");
        Files.writeString(file.toPath(), "content!");
        file.deleteOnExit();

        ClientForm form = new ClientForm();
        form.file = file;
        assertThat(client.postMultipartWithFileContentAsBytes(form)).isEqualTo("content!");
    }

    @Test
    void shouldCopyFileContentToInputStream() throws IOException {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri).build(Client.class);

        File file = File.createTempFile("MultipartTest", ".txt");
        Files.writeString(file.toPath(), "content!");
        file.deleteOnExit();

        ClientForm form = new ClientForm();
        form.file = file;
        assertThat(client.postMultipartWithFileContentAsInputStream(form)).isEqualTo("content!");
    }

    @Path("/multipart")
    @ApplicationScoped
    public static class Resource {
        @POST
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        public String upload(@MultipartForm FormData form) {
            return form.myFile.fileName();
        }

        @POST
        @Path("/file-content")
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        public String uploadWithFileContent(@MultipartForm FormDataWithFileContent form) {
            return form.fileContent;
        }

        @POST
        @Path("/file-content-as-bytes")
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        public String uploadWithFileContentAsBytes(@MultipartForm FormDataWithBytes form) {
            return new String(form.fileContentAsBytes);
        }

        @POST
        @Path("/file-content-as-inputstream")
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        public String uploadWithFileContentAsInputStream(@MultipartForm FormDataWithInputStream form) {
            return new BufferedReader(new InputStreamReader(form.fileContentAsInputStream, StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.joining(System.lineSeparator()));
        }
    }

    public static class FormData {
        @FormParam("myFile")
        public FileUpload myFile;
    }

    public static class FormDataWithFileContent {
        @FormParam("myFile")
        @PartType(MediaType.APPLICATION_OCTET_STREAM)
        public String fileContent;
    }

    public static class FormDataWithBytes {
        @FormParam("myFile")
        @PartType(MediaType.APPLICATION_OCTET_STREAM)
        public byte[] fileContentAsBytes;
    }

    public static class FormDataWithInputStream {
        @FormParam("myFile")
        @PartType(MediaType.APPLICATION_OCTET_STREAM)
        public InputStream fileContentAsInputStream;
    }

    @Path("/multipart")
    public interface Client {
        @POST
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        String postMultipart(@MultipartForm ClientForm clientForm);

        @POST
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        String postMultipartWithPartFilename(@MultipartForm ClientForm2 clientForm);

        @POST
        @Path("/file-content")
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        String postMultipartWithFileContent(@MultipartForm ClientForm clientForm);

        @POST
        @Path("/file-content-as-bytes")
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        String postMultipartWithFileContentAsBytes(@MultipartForm ClientForm clientForm);

        @POST
        @Path("/file-content-as-inputstream")
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        String postMultipartWithFileContentAsInputStream(@MultipartForm ClientForm clientForm);
    }

    public static class ClientForm {
        @FormParam("myFile")
        @PartType(MediaType.APPLICATION_OCTET_STREAM)
        public File file;
    }

    public static class ClientForm2 {
        public static final String FILE_NAME = "clientFile";

        @FormParam("myFile")
        @PartType(MediaType.APPLICATION_OCTET_STREAM)
        @PartFilename(FILE_NAME)
        public File file;
    }
}
