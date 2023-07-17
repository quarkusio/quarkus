package io.quarkus.it.rest.client.multipart;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.MultipartForm;
import org.jboss.resteasy.reactive.PartType;
import org.jboss.resteasy.reactive.RestQuery;
import org.jboss.resteasy.reactive.RestResponse;

import io.quarkus.it.rest.client.multipart.MultipartClient.FileWithPojo;
import io.quarkus.it.rest.client.multipart.MultipartClient.Pojo;
import io.quarkus.it.rest.client.multipart.MultipartClient.WithBufferAsBinaryFile;
import io.quarkus.it.rest.client.multipart.MultipartClient.WithBufferAsTextFile;
import io.quarkus.it.rest.client.multipart.MultipartClient.WithByteArrayAsBinaryFile;
import io.quarkus.it.rest.client.multipart.MultipartClient.WithByteArrayAsTextFile;
import io.quarkus.it.rest.client.multipart.MultipartClient.WithFileAsBinaryFile;
import io.quarkus.it.rest.client.multipart.MultipartClient.WithFileAsTextFile;
import io.quarkus.it.rest.client.multipart.MultipartClient.WithMultiByteAsBinaryFile;
import io.quarkus.it.rest.client.multipart.MultipartClient.WithPathAsBinaryFile;
import io.quarkus.it.rest.client.multipart.MultipartClient.WithPathAsTextFile;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Multi;
import io.vertx.core.buffer.Buffer;

@Path("")
public class MultipartResource {

    private static final Logger log = Logger.getLogger(MultipartResource.class);

    public static final String HELLO_WORLD = "HELLO WORLD";
    public static final String GREETING_TXT = "greeting.txt";
    public static final int NUMBER = 12342;
    @RestClient
    MultipartClient client;

    @GET
    @Path("/client/octet-stream")
    @Produces(MediaType.TEXT_PLAIN)
    @Blocking
    public String sendOctetStreamFile() throws IOException {
        java.nio.file.Path tempFile = Files.createTempFile("dummy", ".txt");
        Files.write(tempFile, "test".getBytes(UTF_8));
        return client.octetStreamFile(tempFile.toFile());
    }

    @GET
    @Path("/client/byte-array-as-binary-file-with-pojo")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    @Blocking
    public String sendByteArrayWithPojo(@RestQuery @DefaultValue("true") Boolean withPojo) {
        FileWithPojo data = new FileWithPojo();
        data.file = HELLO_WORLD.getBytes(UTF_8);
        data.setFileName(GREETING_TXT);
        data.setUuid(UUID.randomUUID());
        if (withPojo) {
            Pojo pojo = new Pojo();
            pojo.setName("some-name");
            pojo.setValue("some-value");
            data.setPojo(pojo);
        }
        try {
            return client.sendFileWithPojo(data);
        } catch (WebApplicationException e) {
            String responseAsString = e.getResponse().readEntity(String.class);
            return String.format("Error: %s statusCode %s", responseAsString, e.getResponse().getStatus());
        }
    }

    @GET
    @Path("/client/params/byte-array-as-binary-file-with-pojo")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    @Blocking
    public String sendByteArrayWithPojoParams(@RestQuery @DefaultValue("true") Boolean withPojo) {
        FileWithPojo data = new FileWithPojo();
        byte[] file = HELLO_WORLD.getBytes(UTF_8);
        String fileName = GREETING_TXT;
        Pojo pojo = null;
        if (withPojo) {
            pojo = new Pojo();
            pojo.setName("some-name");
            pojo.setValue("some-value");
        }
        try {
            return client.sendFileWithPojo(file, fileName, pojo);
        } catch (WebApplicationException e) {
            String responseAsString = e.getResponse().readEntity(String.class);
            return String.format("Error: %s statusCode %s", responseAsString, e.getResponse().getStatus());
        }
    }

    @GET
    @Path("/client/byte-array-as-binary-file")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    @Blocking
    public String sendByteArray(@QueryParam("nullFile") @DefaultValue("false") boolean nullFile) {
        WithByteArrayAsBinaryFile data = new WithByteArrayAsBinaryFile();
        if (!nullFile) {
            data.file = HELLO_WORLD.getBytes(UTF_8);
        }
        data.fileName = GREETING_TXT;
        return client.sendByteArrayAsBinaryFile(data);
    }

    @GET
    @Path("/client/params/byte-array-as-binary-file")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    @Blocking
    public String sendByteArrayParams(@QueryParam("nullFile") @DefaultValue("false") boolean nullFile) {
        byte[] file = null;
        if (!nullFile) {
            file = HELLO_WORLD.getBytes(UTF_8);
        }
        String fileName = GREETING_TXT;
        return client.sendByteArrayAsBinaryFile(file, fileName);
    }

    @GET
    @Path("/client/multi-byte-as-binary-file")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    @Blocking
    public String sendMultiByte(@QueryParam("nullFile") @DefaultValue("false") boolean nullFile) {
        WithMultiByteAsBinaryFile data = new WithMultiByteAsBinaryFile();
        if (!nullFile) {
            List<Byte> bytes = new ArrayList<>();
            for (byte b : HELLO_WORLD.getBytes(UTF_8)) {
                bytes.add(b);
            }

            data.file = Multi.createFrom().iterable(bytes);
        }
        data.fileName = GREETING_TXT;
        return client.sendMultiByteAsBinaryFile(data);
    }

    @GET
    @Path("/client/params/multi-byte-as-binary-file")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    @Blocking
    public String sendMultiByteParams(@QueryParam("nullFile") @DefaultValue("false") boolean nullFile) {
        Multi<Byte> file = null;
        if (!nullFile) {
            List<Byte> bytes = new ArrayList<>();
            for (byte b : HELLO_WORLD.getBytes(UTF_8)) {
                bytes.add(b);
            }

            file = Multi.createFrom().iterable(bytes);
        }
        String fileName = GREETING_TXT;
        return client.sendMultiByteAsBinaryFile(file, fileName);
    }

    @GET
    @Path("/client/buffer-as-binary-file")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    @Blocking
    public String sendBuffer(@QueryParam("nullFile") @DefaultValue("false") boolean nullFile) {
        WithBufferAsBinaryFile data = new WithBufferAsBinaryFile();
        if (!nullFile) {
            data.file = Buffer.buffer(HELLO_WORLD);
        }
        data.fileName = GREETING_TXT;
        return client.sendBufferAsBinaryFile(data);
    }

    @GET
    @Path("/client/params/buffer-as-binary-file")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    @Blocking
    public String sendBufferParams(@QueryParam("nullFile") @DefaultValue("false") boolean nullFile) {
        Buffer file = null;
        if (!nullFile) {
            file = Buffer.buffer(HELLO_WORLD);
        }
        String fileName = GREETING_TXT;
        return client.sendBufferAsBinaryFile(file, fileName);
    }

    @GET
    @Path("/client/file-as-binary-file")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    @Blocking
    public String sendFileAsBinary(@QueryParam("nullFile") @DefaultValue("false") boolean nullFile) throws IOException {
        WithFileAsBinaryFile data = new WithFileAsBinaryFile();

        if (!nullFile) {
            File tempFile = createTempHelloWorldFile();

            data.file = tempFile;
        }
        data.fileName = GREETING_TXT;
        return client.sendFileAsBinaryFile(data);
    }

    @GET
    @Path("/client/params/file-as-binary-file")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    @Blocking
    public String sendFileAsBinaryParams(@QueryParam("nullFile") @DefaultValue("false") boolean nullFile) throws IOException {
        File file = null;
        if (!nullFile) {
            File tempFile = createTempHelloWorldFile();

            file = tempFile;
        }
        String fileName = GREETING_TXT;
        return client.sendFileAsBinaryFile(file, fileName);
    }

    @GET
    @Path("/client/path-as-binary-file")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    @Blocking
    public String sendPathAsBinary(@QueryParam("nullFile") @DefaultValue("false") boolean nullFile) throws IOException {
        WithPathAsBinaryFile data = new WithPathAsBinaryFile();

        if (!nullFile) {
            File tempFile = createTempHelloWorldFile();

            data.file = tempFile.toPath();
        }
        data.fileName = GREETING_TXT;
        return client.sendPathAsBinaryFile(data);
    }

    @GET
    @Path("/client/params/path-as-binary-file")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    @Blocking
    public String sendPathAsBinaryParams(@QueryParam("nullFile") @DefaultValue("false") boolean nullFile) throws IOException {
        java.nio.file.Path file = null;
        if (!nullFile) {
            File tempFile = createTempHelloWorldFile();

            file = tempFile.toPath();
        }
        String fileName = GREETING_TXT;
        return client.sendPathAsBinaryFile(file, fileName);
    }

    @GET
    @Path("/client/byte-array-as-text-file")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    @Blocking
    public String sendByteArrayAsTextFile() {
        WithByteArrayAsTextFile data = new WithByteArrayAsTextFile();
        data.file = HELLO_WORLD.getBytes(UTF_8);
        data.number = NUMBER;
        return client.sendByteArrayAsTextFile(data);
    }

    @GET
    @Path("/client/params/byte-array-as-text-file")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    @Blocking
    public String sendByteArrayAsTextFileParams() {
        byte[] file = HELLO_WORLD.getBytes(UTF_8);
        int number = NUMBER;
        return client.sendByteArrayAsTextFile(file, number);
    }

    @GET
    @Path("/client/buffer-as-text-file")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    @Blocking
    public String sendBufferAsTextFile() {
        WithBufferAsTextFile data = new WithBufferAsTextFile();
        data.file = Buffer.buffer(HELLO_WORLD);
        data.number = NUMBER;
        return client.sendBufferAsTextFile(data);
    }

    @GET
    @Path("/client/params/buffer-as-text-file")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    @Blocking
    public String sendBufferAsTextFileParams() {
        Buffer file = Buffer.buffer(HELLO_WORLD);
        int number = NUMBER;
        return client.sendBufferAsTextFile(file, number);
    }

    @GET
    @Path("/client/file-as-text-file")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    @Blocking
    public String sendFileAsText() throws IOException {
        File tempFile = createTempHelloWorldFile();

        WithFileAsTextFile data = new WithFileAsTextFile();
        data.file = tempFile;
        data.number = NUMBER;
        return client.sendFileAsTextFile(data);
    }

    @GET
    @Path("/client/params/file-as-text-file")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    @Blocking
    public String sendFileAsTextParams() throws IOException {
        File tempFile = createTempHelloWorldFile();

        File file = tempFile;
        int number = NUMBER;
        return client.sendFileAsTextFile(file, number);
    }

    @GET
    @Path("/client/path-as-text-file")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    @Blocking
    public String sendPathAsText() throws IOException {
        File tempFile = createTempHelloWorldFile();

        WithPathAsTextFile data = new WithPathAsTextFile();
        data.file = tempFile.toPath();
        data.number = NUMBER;
        return client.sendPathAsTextFile(data);
    }

    @GET
    @Path("/client/params/path-as-text-file")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    @Blocking
    public String sendPathAsTextParams() throws IOException {
        File tempFile = createTempHelloWorldFile();

        java.nio.file.Path file = tempFile.toPath();
        int number = NUMBER;
        return client.sendPathAsTextFile(file, number);
    }

    @POST
    @Path("/echo/octet-stream")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    public String consumeOctetStream(File file) throws IOException {
        return Files.readString(file.toPath());
    }

    @POST
    @Path("/echo/binary")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public String consumeMultipart(@MultipartForm MultipartBodyWithBinaryFile body) {
        return String.format("fileOk:%s,nameOk:%s", body.file == null ? "null" : containsHelloWorld(body.file),
                GREETING_TXT.equals(body.fileName));
    }

    @POST
    @Path("/echo/text")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public String consumeText(@MultipartForm MultipartBodyWithTextFile2 body) {
        return String.format("fileOk:%s,numberOk:%s", containsHelloWorld(body.file),
                NUMBER == Integer.parseInt(body.number[0]));
    }

    @POST
    @Path("/echo/with-pojo")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public String consumeBinaryWithPojo(@MultipartForm MultipartBodyWithBinaryFileAndPojo fileWithPojo) {
        return String.format("fileOk:%s,nameOk:%s,pojoOk:%s,uuidNull:%s",
                containsHelloWorld(fileWithPojo.file),
                GREETING_TXT.equals(fileWithPojo.fileName),
                fileWithPojo.pojo == null ? "null"
                        : "some-name".equals(fileWithPojo.pojo.getName()) && "some-value".equals(fileWithPojo.pojo.getValue()),
                fileWithPojo.uuid == null);
    }

    @GET
    @Path("/produces/multipart")
    @Produces(MediaType.MULTIPART_FORM_DATA)
    public MultipartBodyWithTextFile produceMultipart() throws IOException {
        File tempFile = createTempHelloWorldFile();

        MultipartBodyWithTextFile data = new MultipartBodyWithTextFile();
        data.file = tempFile;
        data.number = String.valueOf(NUMBER);
        return data;
    }

    @GET
    @Path("/produces/input-stream-rest-response")
    public RestResponse<? extends InputStream> produceInputStreamRestResponse() throws IOException {
        File tempFile = createTempHelloWorldFile();
        FileInputStream is = new FileInputStream(tempFile);
        return RestResponse.ResponseBuilder
                .ok(is)
                .type(MediaType.TEXT_PLAIN_TYPE)
                .build();
    }

    private File createTempHelloWorldFile() throws IOException {
        File tempFile = File.createTempFile("quarkus-test", ".bin");
        tempFile.deleteOnExit();

        try (FileOutputStream fileOutputStream = new FileOutputStream(tempFile)) {
            fileOutputStream.write(HELLO_WORLD.getBytes());
        }
        return tempFile;
    }

    private boolean containsHelloWorld(File file) {
        try {
            String actual = new String(Files.readAllBytes(file.toPath()));
            return HELLO_WORLD.equals(actual);
        } catch (IOException e) {
            log.error("Failed to contents of uploaded file " + file.getAbsolutePath());
            return false;
        }
    }

    public static class MultipartBodyWithBinaryFile {

        @FormParam("file")
        @PartType(MediaType.APPLICATION_OCTET_STREAM)
        public File file;

        @FormParam("fileName")
        @PartType(MediaType.TEXT_PLAIN)
        public String fileName;
    }

    public static class MultipartBodyWithTextFile {

        @FormParam("file")
        @PartType(MediaType.TEXT_PLAIN)
        public File file;

        @FormParam("number")
        @PartType(MediaType.TEXT_PLAIN)
        public String number;
    }

    public static class MultipartBodyWithTextFile2 {

        @FormParam("file")
        @PartType(MediaType.TEXT_PLAIN)
        public File file;

        @FormParam("number")
        @PartType(MediaType.TEXT_PLAIN)
        public String[] number;
    }

    public static class MultipartBodyWithBinaryFileAndPojo {

        @FormParam("file")
        @PartType(MediaType.APPLICATION_OCTET_STREAM)
        public File file;

        @FormParam("fileName")
        @PartType(MediaType.TEXT_PLAIN)
        public String fileName;

        @FormParam("pojo")
        @PartType(MediaType.APPLICATION_JSON)
        public Pojo pojo;

        @FormParam("uuid")
        @PartType(MediaType.TEXT_PLAIN)
        public String uuid;
    }

}
