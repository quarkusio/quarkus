package io.quarkus.it.rest.client.multipart;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.MultipartForm;
import org.jboss.resteasy.reactive.PartType;
import org.jboss.resteasy.reactive.RestQuery;

import io.quarkus.it.rest.client.multipart.MultipartClient.FileWithPojo;
import io.quarkus.it.rest.client.multipart.MultipartClient.Pojo;
import io.quarkus.it.rest.client.multipart.MultipartClient.WithBufferAsBinaryFile;
import io.quarkus.it.rest.client.multipart.MultipartClient.WithBufferAsTextFile;
import io.quarkus.it.rest.client.multipart.MultipartClient.WithByteArrayAsBinaryFile;
import io.quarkus.it.rest.client.multipart.MultipartClient.WithByteArrayAsTextFile;
import io.quarkus.it.rest.client.multipart.MultipartClient.WithFileAsBinaryFile;
import io.quarkus.it.rest.client.multipart.MultipartClient.WithFileAsTextFile;
import io.quarkus.it.rest.client.multipart.MultipartClient.WithPathAsBinaryFile;
import io.quarkus.it.rest.client.multipart.MultipartClient.WithPathAsTextFile;
import io.smallrye.common.annotation.Blocking;
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
    @Path("/client/byte-array-as-binary-file-with-pojo")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    @Blocking
    public String sendByteArrayWithPojo(@RestQuery @DefaultValue("true") Boolean withPojo) {
        FileWithPojo data = new FileWithPojo();
        data.file = HELLO_WORLD.getBytes(UTF_8);
        data.fileName = GREETING_TXT;
        if (withPojo) {
            data.pojo = new Pojo();
            data.pojo.setName("some-name");
            data.pojo.setValue("some-value");
        }
        try {
            return client.sendFileWithPojo(data);
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
    public String sendByteArray() {
        WithByteArrayAsBinaryFile data = new WithByteArrayAsBinaryFile();
        data.file = HELLO_WORLD.getBytes(UTF_8);
        data.fileName = GREETING_TXT;
        return client.sendByteArrayAsBinaryFile(data);
    }

    @GET
    @Path("/client/buffer-as-binary-file")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    @Blocking
    public String sendBuffer() {
        WithBufferAsBinaryFile data = new WithBufferAsBinaryFile();
        data.file = Buffer.buffer(HELLO_WORLD);
        data.fileName = GREETING_TXT;
        return client.sendBufferAsBinaryFile(data);
    }

    @GET
    @Path("/client/file-as-binary-file")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    @Blocking
    public String sendFileAsBinary() throws IOException {
        File tempFile = File.createTempFile("quarkus-test", ".bin");
        tempFile.deleteOnExit();

        try (FileOutputStream fileOutputStream = new FileOutputStream(tempFile)) {
            fileOutputStream.write(HELLO_WORLD.getBytes());
        }

        WithFileAsBinaryFile data = new WithFileAsBinaryFile();
        data.file = tempFile;
        data.fileName = GREETING_TXT;
        return client.sendFileAsBinaryFile(data);
    }

    @GET
    @Path("/client/path-as-binary-file")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    @Blocking
    public String sendPathAsBinary() throws IOException {
        File tempFile = File.createTempFile("quarkus-test", ".bin");
        tempFile.deleteOnExit();

        try (FileOutputStream fileOutputStream = new FileOutputStream(tempFile)) {
            fileOutputStream.write(HELLO_WORLD.getBytes());
        }

        WithPathAsBinaryFile data = new WithPathAsBinaryFile();
        data.file = tempFile.toPath();
        data.fileName = GREETING_TXT;
        return client.sendPathAsBinaryFile(data);
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
    @Path("/client/file-as-text-file")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    @Blocking
    public String sendFileAsText() throws IOException {
        File tempFile = File.createTempFile("quarkus-test", ".bin");
        tempFile.deleteOnExit();

        try (FileOutputStream fileOutputStream = new FileOutputStream(tempFile)) {
            fileOutputStream.write(HELLO_WORLD.getBytes());
        }

        WithFileAsTextFile data = new WithFileAsTextFile();
        data.file = tempFile;
        data.number = NUMBER;
        return client.sendFileAsTextFile(data);
    }

    @GET
    @Path("/client/path-as-text-file")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    @Blocking
    public String sendPathAsText() throws IOException {
        File tempFile = File.createTempFile("quarkus-test", ".bin");
        tempFile.deleteOnExit();

        try (FileOutputStream fileOutputStream = new FileOutputStream(tempFile)) {
            fileOutputStream.write(HELLO_WORLD.getBytes());
        }

        WithPathAsTextFile data = new WithPathAsTextFile();
        data.file = tempFile.toPath();
        data.number = NUMBER;
        return client.sendPathAsTextFile(data);
    }

    @POST
    @Path("/echo/binary")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public String consumeMultipart(@MultipartForm MultipartBodyWithBinaryFile body) {
        return String.format("fileOk:%s,nameOk:%s", containsHelloWorld(body.file), GREETING_TXT.equals(body.fileName));
    }

    @POST
    @Path("/echo/text")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public String consumeText(@MultipartForm MultipartBodyWithTextFile body) {
        return String.format("fileOk:%s,numberOk:%s", containsHelloWorld(body.file), NUMBER == Integer.parseInt(body.number));
    }

    @POST
    @Path("/echo/with-pojo")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public String consumeBinaryWithPojo(@MultipartForm MultipartBodyWithBinaryFileAndPojo fileWithPojo) {
        return String.format("fileOk:%s,nameOk:%s,pojoOk:%s",
                containsHelloWorld(fileWithPojo.file),
                GREETING_TXT.equals(fileWithPojo.fileName),
                fileWithPojo.pojo == null ? "null"
                        : "some-name".equals(fileWithPojo.pojo.getName()) && "some-value".equals(fileWithPojo.pojo.getValue()));
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
    }

}
