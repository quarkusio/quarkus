package io.quarkus.rest.client.reactive.multipart;

import static jakarta.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.jboss.resteasy.reactive.MultipartForm;
import org.jboss.resteasy.reactive.PartFilename;
import org.jboss.resteasy.reactive.PartType;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.client.impl.multipart.PausableHttpPostRequestEncoder;
import org.jboss.resteasy.reactive.common.util.QuarkusMultivaluedHashMap;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.jboss.resteasy.reactive.server.multipart.FormValue;
import org.jboss.resteasy.reactive.server.multipart.MultipartFormDataInput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.smallrye.mutiny.Multi;

public class MultipartFilenameTest {

    public static final String FILE_NAME = "clientFile";
    public static final String FILE_CONTENT = "file content";
    public static final String FILE_CONTENT2 = "file content2";
    public static final String EXPECTED_OUTPUT = FILE_NAME + ":" + FILE_CONTENT;

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
    void shouldWorkWithFileUpload() throws IOException {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri).build(Client.class);

        File file = File.createTempFile("MultipartTest", ".txt");
        file.deleteOnExit();

        ClientFormUsingFileUpload form = new ClientFormUsingFileUpload();
        form.file = new FileUpload() {

            @Override
            public String name() {
                return "myFile";
            }

            @Override
            public java.nio.file.Path filePath() {
                return file.toPath();
            }

            @Override
            public String fileName() {
                return file.getName();
            }

            @Override
            public long size() {
                return 0;
            }

            @Override
            public String contentType() {
                return "application/octet-stream";
            }

            @Override
            public String charSet() {
                return "";
            }

            @Override
            public MultivaluedMap<String, String> getHeaders() {
                return new QuarkusMultivaluedHashMap<>();
            }
        };
        assertThat(client.postMultipartFileUpload(form)).isEqualTo(file.getName());
    }

    @Test
    void shouldUseFileNameFromAnnotation() throws IOException {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri).build(Client.class);

        File file = File.createTempFile("MultipartTest", ".txt");
        file.deleteOnExit();

        ClientFormUsingFile form = new ClientFormUsingFile();
        form.file = file;
        // Using a Multipart bean
        assertThat(client.postMultipartWithPartFilename(form)).isEqualTo(FILE_NAME);
        // Using a field form param
        assertThat(client.postMultipartWithPartFilename(file)).isEqualTo(FILE_NAME);
    }

    @Test
    void shouldUseFileNameFromAnnotationUsingString() {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri).build(Client.class);

        ClientFormUsingString form = new ClientFormUsingString();
        form.file = FILE_CONTENT;
        // Using a Multipart bean
        assertThat(client.postMultipartWithPartFilenameUsingString(form)).isEqualTo(EXPECTED_OUTPUT);
        // Using a field form param
        assertThat(client.postMultipartWithPartFilenameUsingString(FILE_CONTENT)).isEqualTo(EXPECTED_OUTPUT);
    }

    @Test
    void shouldUseFileNameFromAnnotationUsingByteArray() {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri).build(Client.class);

        ClientFormUsingByteArray form = new ClientFormUsingByteArray();
        form.file = FILE_CONTENT.getBytes(StandardCharsets.UTF_8);
        // Using a Multipart bean
        assertThat(client.postMultipartWithPartFilenameUsingByteArray(form)).isEqualTo(EXPECTED_OUTPUT);
        // Using a field form param
        assertThat(client.postMultipartWithPartFilenameUsingByteArray(form.file)).isEqualTo(EXPECTED_OUTPUT);
    }

    @Test
    void shouldUseFileNameFromAnnotationUsingInputStream() {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri).build(Client.class);

        var bytes = FILE_CONTENT.getBytes(StandardCharsets.UTF_8);
        ClientFormUsingInputStream form = new ClientFormUsingInputStream();
        form.file = new ByteArrayInputStream(bytes);
        // Using a Multipart bean
        assertThat(client.postMultipartWithPartFilenameUsingInputStream(form)).isEqualTo(EXPECTED_OUTPUT);
        // Using a field form param
        assertThat(client.postMultipartWithPartFilenameUsingInputStream(new ByteArrayInputStream(bytes)))
                .isEqualTo(EXPECTED_OUTPUT);
        // Using rest data annotation without filename
        ClientRestFormUsingInputStream restForm = new ClientRestFormUsingInputStream();
        restForm.file = new ByteArrayInputStream(bytes);
        assertThat(client.postMultipartWithPartFilenameUsingInputStream(restForm)).isEqualTo("myFile:" + FILE_CONTENT);
    }

    @Test
    void shouldUseFileNameFromAnnotationUsingMultiByte() {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri).build(Client.class);

        var list = new ArrayList<Byte>();
        var array = FILE_CONTENT.getBytes(StandardCharsets.UTF_8);
        for (var b : array) {
            list.add(b);
        }

        ClientFormUsingMultiByte form = new ClientFormUsingMultiByte();
        form.file = Multi.createFrom().items(list.stream());
        // Using a Multipart bean
        assertThat(client.postMultipartWithPartFilenameUsingMultiByte(form)).isEqualTo(EXPECTED_OUTPUT);
        // Using a field form param
        assertThat(client.postMultipartWithPartFilenameUsingMultiByte(Multi.createFrom().items(list.stream())))
                .isEqualTo(EXPECTED_OUTPUT);
    }

    @Test
    void shouldCopyFileContentToString() throws IOException {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri).build(Client.class);

        File file = File.createTempFile("MultipartTest", ".txt");
        Files.writeString(file.toPath(), FILE_CONTENT);
        file.deleteOnExit();

        ClientForm form = new ClientForm();
        form.file = file;
        assertThat(client.postMultipartWithFileContent(form)).isEqualTo(FILE_CONTENT);
    }

    @Test
    void shouldWorkWithListOfFiles() throws IOException {
        Client client = RestClientBuilder.newBuilder()
                .baseUri(baseUri)
                // we use this encoder mode on the client in order to make it possible for the server to read items with the same name
                .property("io.quarkus.rest.client.multipart-post-encoder-mode",
                        PausableHttpPostRequestEncoder.EncoderMode.HTML5)
                .build(Client.class);

        File file = File.createTempFile("MultipartTest", ".txt");
        Files.writeString(file.toPath(), FILE_CONTENT);
        file.deleteOnExit();
        ClientListForm form = new ClientListForm();
        form.files = List.of(file);

        String responseStr = client.postMultipartWithFileContentAsMultipartFormDataInput(form);

        ObjectMapper mapper = new ObjectMapper();

        Result result = mapper.readValue(responseStr, Result.class);
        assertThat(result).satisfies(r -> {
            assertThat(r.count).isEqualTo(1);
            assertThat(r.items).singleElement().satisfies(i -> {
                assertThat(i.name).isEqualTo("myFile");
                assertThat(i.size).isEqualTo(FILE_CONTENT.length());
                assertThat(i.isFileItem).isEqualTo(true);
            });
        });

        File file2 = File.createTempFile("MultipartTest2", ".txt");
        Files.writeString(file2.toPath(), FILE_CONTENT2);
        file2.deleteOnExit();
        form = new ClientListForm();
        form.files = List.of(file, file2);

        responseStr = client.postMultipartWithFileContentAsMultipartFormDataInput(form);

        result = mapper.readValue(responseStr, Result.class);
        assertThat(result).satisfies(r -> {
            assertThat(r.count).isEqualTo(2);
            assertThat(r.items).hasSize(2).extracting(Item::name).containsOnly("myFile");
            assertThat(r.items).hasSize(2).extracting(Item::size).containsOnly((long) FILE_CONTENT.length(),
                    (long) FILE_CONTENT2.length());
        });
    }

    @Test
    void shouldCopyFileContentToBytes() throws IOException {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri).build(Client.class);

        File file = File.createTempFile("MultipartTest", ".txt");
        Files.writeString(file.toPath(), FILE_CONTENT);
        file.deleteOnExit();

        ClientForm form = new ClientForm();
        form.file = file;
        assertThat(client.postMultipartWithFileContentAsBytes(form)).isEqualTo(FILE_CONTENT);
    }

    @Test
    void shouldCopyFileContentToInputStream() throws IOException {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri).build(Client.class);

        File file = File.createTempFile("MultipartTest", ".txt");
        Files.writeString(file.toPath(), FILE_CONTENT);
        file.deleteOnExit();

        ClientForm form = new ClientForm();
        form.file = file;
        assertThat(client.postMultipartWithFileContentAsInputStream(form)).isEqualTo(FILE_CONTENT);
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
        @Path("/using-form-data")
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        public String uploadWithFileContentUsingString(@MultipartForm FormData form) throws IOException {
            return form.myFile.fileName() + ":" + Files.readString(form.myFile.uploadedFile());
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

        @POST
        @Path("/file-content-as-multipart-form-data-input")
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        public String uploadMultipartFormDataInput(MultipartFormDataInput input) throws IOException {
            Map<String, Collection<FormValue>> map = input.getValues();
            List<Item> items = new ArrayList<>();
            for (var entry : map.entrySet()) {
                for (FormValue value : entry.getValue()) {
                    items.add(new Item(
                            entry.getKey(),
                            value.isFileItem() ? value.getFileItem().getFileSize() : value.getValue().length(),
                            value.getCharset(),
                            value.isFileItem()));
                }

            }
            return new ObjectMapper().writeValueAsString(new Result(items, items.size()));
        }
    }

    public static class FormData {
        @FormParam("myFile")
        public FileUpload myFile;
    }

    public static class FormDataWithFileContent {
        @FormParam("myFile")
        @PartType(APPLICATION_OCTET_STREAM)
        public String fileContent;
    }

    public static class FormDataWithBytes {
        @FormParam("myFile")
        @PartType(APPLICATION_OCTET_STREAM)
        public byte[] fileContentAsBytes;
    }

    public static class FormDataWithInputStream {
        @FormParam("myFile")
        @PartType(APPLICATION_OCTET_STREAM)
        public InputStream fileContentAsInputStream;
    }

    @Path("/multipart")
    public interface Client {
        @POST
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        String postMultipart(@MultipartForm ClientForm clientForm);

        @POST
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        String postMultipartFileUpload(ClientFormUsingFileUpload clientForm);

        @POST
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        String postMultipartWithPartFilename(@MultipartForm ClientFormUsingFile clientForm);

        @POST
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        String postMultipartWithPartFilename(
                @FormParam("myFile") @PartType(APPLICATION_OCTET_STREAM) @PartFilename(FILE_NAME) File file);

        @POST
        @Path("/using-form-data")
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        String postMultipartWithPartFilenameUsingString(@MultipartForm ClientFormUsingString clientForm);

        @POST
        @Path("/using-form-data")
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        String postMultipartWithPartFilenameUsingString(
                @RestForm @PartType(APPLICATION_OCTET_STREAM) @PartFilename(FILE_NAME) String myFile);

        @POST
        @Path("/using-form-data")
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        String postMultipartWithPartFilenameUsingByteArray(@MultipartForm ClientFormUsingByteArray clientForm);

        @POST
        @Path("/using-form-data")
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        String postMultipartWithPartFilenameUsingByteArray(
                @FormParam("myFile") @PartType(APPLICATION_OCTET_STREAM) @PartFilename(FILE_NAME) byte[] file);

        @POST
        @Path("/using-form-data")
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        String postMultipartWithPartFilenameUsingInputStream(@MultipartForm ClientFormUsingInputStream clientForm);

        @POST
        @Path("/using-form-data")
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        String postMultipartWithPartFilenameUsingInputStream(@MultipartForm ClientRestFormUsingInputStream clientForm);

        @POST
        @Path("/using-form-data")
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        String postMultipartWithPartFilenameUsingInputStream(
                @FormParam("myFile") @PartType(APPLICATION_OCTET_STREAM) @PartFilename(FILE_NAME) InputStream file);

        @POST
        @Path("/using-form-data")
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        String postMultipartWithPartFilenameUsingMultiByte(@MultipartForm ClientFormUsingMultiByte clientForm);

        @POST
        @Path("/using-form-data")
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        String postMultipartWithPartFilenameUsingMultiByte(
                @FormParam("myFile") @PartType(APPLICATION_OCTET_STREAM) @PartFilename(FILE_NAME) Multi<Byte> file);

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

        @POST
        @Path("/file-content-as-multipart-form-data-input")
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        String postMultipartWithFileContentAsMultipartFormDataInput(@MultipartForm ClientListForm clientForm);
    }

    public static class ClientForm {
        @FormParam("myFile")
        @PartType(APPLICATION_OCTET_STREAM)
        public File file;
    }

    public static class ClientListForm {
        @FormParam("myFile")
        @PartType(APPLICATION_OCTET_STREAM)
        public List<File> files;
    }

    public static class ClientFormUsingFileUpload {
        @RestForm
        public FileUpload file;
    }

    public static class ClientFormUsingFile {
        @FormParam("myFile")
        @PartType(APPLICATION_OCTET_STREAM)
        @PartFilename(FILE_NAME)
        public File file;
    }

    public static class ClientFormUsingString {
        public static final String FILE_NAME = "clientFile";

        @FormParam("myFile")
        @PartType(APPLICATION_OCTET_STREAM)
        @PartFilename(FILE_NAME)
        public String file;
    }

    public static class ClientFormUsingByteArray {
        @FormParam("myFile")
        @PartType(APPLICATION_OCTET_STREAM)
        @PartFilename(FILE_NAME)
        public byte[] file;
    }

    public static class ClientFormUsingInputStream {
        @FormParam("myFile")
        @PartFilename(FILE_NAME)
        public InputStream file;
    }

    public static class ClientRestFormUsingInputStream {
        @RestForm("myFile")
        @PartType(APPLICATION_OCTET_STREAM)
        public InputStream file;
    }

    public static class ClientFormUsingMultiByte {
        @FormParam("myFile")
        @PartType(APPLICATION_OCTET_STREAM)
        @PartFilename(FILE_NAME)
        public Multi<Byte> file;
    }

    public record Item(String name, long size, String charset, boolean isFileItem) {
    }

    public static class Result {
        public List<Item> items;
        public int count;

        public Result() {
        }

        public Result(List<Item> items, int count) {
            this.items = items;
            this.count = count;
        }
    }
}
