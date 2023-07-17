package io.quarkus.it.rest.client.multipart;

import java.io.File;
import java.util.UUID;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.reactive.MultipartForm;
import org.jboss.resteasy.reactive.PartType;

import io.smallrye.mutiny.Multi;
import io.vertx.core.buffer.Buffer;

@Path("/echo")
@RegisterRestClient(configKey = "multipart-client")
public interface MultipartClient {

    @POST
    @Path("/octet-stream")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.TEXT_PLAIN)
    String octetStreamFile(File body);

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/binary")
    String sendByteArrayAsBinaryFile(@MultipartForm WithByteArrayAsBinaryFile data);

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/binary")
    String sendByteArrayAsBinaryFile(@FormParam("file") @PartType(MediaType.APPLICATION_OCTET_STREAM) byte[] file,

            @FormParam("fileName") @PartType(MediaType.TEXT_PLAIN) String fileName);

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/binary")
    String sendMultiByteAsBinaryFile(@MultipartForm WithMultiByteAsBinaryFile data);

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/binary")
    String sendMultiByteAsBinaryFile(@FormParam("file") @PartType(MediaType.APPLICATION_OCTET_STREAM) Multi<Byte> file,

            @FormParam("fileName") @PartType(MediaType.TEXT_PLAIN) String fileName);

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/binary")
    String sendBufferAsBinaryFile(@MultipartForm WithBufferAsBinaryFile data);

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/binary")
    String sendBufferAsBinaryFile(@FormParam("file") @PartType(MediaType.APPLICATION_OCTET_STREAM) Buffer file,

            @FormParam("fileName") @PartType(MediaType.TEXT_PLAIN) String fileName);

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/binary")
    String sendFileAsBinaryFile(@MultipartForm WithFileAsBinaryFile data);

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/binary")
    String sendFileAsBinaryFile(@FormParam("file") @PartType(MediaType.APPLICATION_OCTET_STREAM) File file,

            @FormParam("fileName") @PartType(MediaType.TEXT_PLAIN) String fileName);

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/binary")
    String sendPathAsBinaryFile(@MultipartForm WithPathAsBinaryFile data);

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/binary")
    String sendPathAsBinaryFile(@FormParam("file") @PartType(MediaType.APPLICATION_OCTET_STREAM) java.nio.file.Path file,

            @FormParam("fileName") @PartType(MediaType.TEXT_PLAIN) String fileName);

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/text")
    String sendByteArrayAsTextFile(@MultipartForm WithByteArrayAsTextFile data);

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/text")
    String sendByteArrayAsTextFile(@FormParam("file") @PartType(MediaType.TEXT_PLAIN) byte[] file,

            @FormParam("number") @PartType(MediaType.TEXT_PLAIN) int number);

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/text")
    String sendBufferAsTextFile(@MultipartForm WithBufferAsTextFile data);

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/text")
    String sendBufferAsTextFile(@FormParam("file") @PartType(MediaType.APPLICATION_OCTET_STREAM) Buffer file,

            @FormParam("number") @PartType(MediaType.TEXT_PLAIN) int number);

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/text")
    String sendFileAsTextFile(@MultipartForm WithFileAsTextFile data);

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/text")
    String sendFileAsTextFile(@FormParam("file") @PartType(MediaType.TEXT_PLAIN) File file,

            @FormParam("number") @PartType(MediaType.TEXT_PLAIN) int number);

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/text")
    String sendPathAsTextFile(@MultipartForm WithPathAsTextFile data);

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/text")
    String sendPathAsTextFile(@FormParam("file") @PartType(MediaType.TEXT_PLAIN) java.nio.file.Path file,

            @FormParam("number") @PartType(MediaType.TEXT_PLAIN) int number);

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Path("/with-pojo")
    String sendFileWithPojo(@MultipartForm FileWithPojo data);

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Path("/with-pojo")
    String sendFileWithPojo(@FormParam("file") @PartType(MediaType.APPLICATION_OCTET_STREAM) byte[] file,

            @FormParam("fileName") @PartType(MediaType.TEXT_PLAIN) String fileName,

            @FormParam("pojo") @PartType(MediaType.APPLICATION_JSON) Pojo pojo);

    class FileWithPojo {
        @FormParam("file")
        @PartType(MediaType.APPLICATION_OCTET_STREAM)
        public byte[] file;

        @FormParam("fileName")
        @PartType(MediaType.TEXT_PLAIN)
        private String fileName;

        @FormParam("pojo")
        @PartType(MediaType.APPLICATION_JSON)
        private Pojo pojo;

        @FormParam("uuid")
        @PartType(MediaType.TEXT_PLAIN)
        private UUID uuid;

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public Pojo getPojo() {
            return pojo;
        }

        public void setPojo(Pojo pojo) {
            this.pojo = pojo;
        }

        public UUID getUuid() {
            return uuid;
        }

        public void setUuid(UUID uuid) {
            this.uuid = uuid;
        }
    }

    class Pojo {
        private String name;
        private String value;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    class WithByteArrayAsBinaryFile {

        @FormParam("file")
        @PartType(MediaType.APPLICATION_OCTET_STREAM)
        public byte[] file;

        @FormParam("fileName")
        @PartType(MediaType.TEXT_PLAIN)
        public String fileName;
    }

    class WithMultiByteAsBinaryFile {

        @FormParam("file")
        @PartType(MediaType.APPLICATION_OCTET_STREAM)
        public Multi<Byte> file;

        @FormParam("fileName")
        @PartType(MediaType.TEXT_PLAIN)
        public String fileName;
    }

    class WithBufferAsBinaryFile {
        @FormParam("file")
        @PartType(MediaType.APPLICATION_OCTET_STREAM)
        public Buffer file;

        @FormParam("fileName")
        @PartType(MediaType.TEXT_PLAIN)
        public String fileName;
    }

    class WithFileAsBinaryFile {
        @FormParam("file")
        @PartType(MediaType.APPLICATION_OCTET_STREAM)
        public File file;

        @FormParam("fileName")
        @PartType(MediaType.TEXT_PLAIN)
        public String fileName;
    }

    class WithPathAsBinaryFile {
        @FormParam("file")
        @PartType(MediaType.APPLICATION_OCTET_STREAM)
        public java.nio.file.Path file;

        @FormParam("fileName")
        @PartType(MediaType.TEXT_PLAIN)
        public String fileName;
    }

    class WithPathAsTextFile {
        @FormParam("file")
        @PartType(MediaType.TEXT_PLAIN)
        public java.nio.file.Path file;

        @FormParam("number")
        @PartType(MediaType.TEXT_PLAIN)
        public int number;
    }

    class WithByteArrayAsTextFile {

        @FormParam("file")
        @PartType(MediaType.TEXT_PLAIN)
        public byte[] file;

        @FormParam("number")
        @PartType(MediaType.TEXT_PLAIN)
        public int number;
    }

    class WithBufferAsTextFile {
        @FormParam("file")
        @PartType(MediaType.APPLICATION_OCTET_STREAM)
        public Buffer file;

        @FormParam("number")
        @PartType(MediaType.TEXT_PLAIN)
        public int number;
    }

    class WithFileAsTextFile {
        @FormParam("file")
        @PartType(MediaType.TEXT_PLAIN)
        public File file;

        @FormParam("number")
        @PartType(MediaType.TEXT_PLAIN)
        public int number;
    }
}
