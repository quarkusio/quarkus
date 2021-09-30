package io.quarkus.it.rest.client.multipart;

import java.io.File;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.reactive.MultipartForm;
import org.jboss.resteasy.reactive.PartType;

import io.vertx.core.buffer.Buffer;

@Path("/echo")
@RegisterRestClient(configKey = "multipart-client")
public interface MultipartClient {

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/binary")
    String sendByteArrayAsBinaryFile(@MultipartForm WithByteArrayAsBinaryFile data);

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/binary")
    String sendBufferAsBinaryFile(@MultipartForm WithBufferAsBinaryFile data);

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/binary")
    String sendFileAsBinaryFile(@MultipartForm WithFileAsBinaryFile data);

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/binary")
    String sendPathAsBinaryFile(@MultipartForm WithPathAsBinaryFile data);

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/text")
    String sendByteArrayAsTextFile(@MultipartForm WithByteArrayAsTextFile data);

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/text")
    String sendBufferAsTextFile(@MultipartForm WithBufferAsTextFile data);

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/text")
    String sendFileAsTextFile(@MultipartForm WithFileAsTextFile data);

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/text")
    String sendPathAsTextFile(@MultipartForm WithPathAsTextFile data);

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Path("/with-pojo")
    String sendFileWithPojo(@MultipartForm FileWithPojo data);

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
