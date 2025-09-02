package io.quarkus.rest.client.reactive.multipart;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.jboss.resteasy.reactive.PartType;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.common.util.QuarkusMultivaluedHashMap;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.client.reactive.TestJacksonBasicMessageBodyReader;
import io.quarkus.rest.client.reactive.TestJacksonBasicMessageBodyWriter;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.smallrye.mutiny.Multi;
import io.vertx.core.buffer.Buffer;

public class MultipartDetectionTest {

    @TestHTTPResource
    URI baseUri;

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot(
                    jar -> jar.addClasses(Resource.class, Client.class, Person.class, TestJacksonBasicMessageBodyReader.class,
                            TestJacksonBasicMessageBodyWriter.class));

    @Test
    void shouldCallExplicitEndpoints() throws IOException {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri).build(Client.class);

        File file = File.createTempFile("MultipartTest", ".txt");
        Files.writeString(file.toPath(), "Hello");
        file.deleteOnExit();

        assertThat(client.postMultipartExplicit(file.getName(), file))
                .isEqualTo(file.getName() + " " + file.getName() + " Hello");
        assertThat(client.postUrlencodedExplicit(file.getName())).isEqualTo(file.getName());
    }

    @Test
    void shouldCallImplicitEndpoints() throws IOException {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri).build(Client.class);

        File file = File.createTempFile("MultipartTest", ".txt");
        byte[] contents = "Hello".getBytes(StandardCharsets.UTF_8);
        Files.write(file.toPath(), contents);
        file.deleteOnExit();
        Byte[] contentsForMulti = new Byte[contents.length];
        for (int i = 0; i < contents.length; i++) {
            contentsForMulti[i] = contents[i];
        }
        Person person = new Person();
        person.firstName = "Stef";
        person.lastName = "Epardaud";

        assertThat(client.postMultipartImplicit(file.getName(), file))
                .isEqualTo(file.getName() + " " + file.getName() + " Hello");
        assertThat(client.postMultipartImplicit(file.getName(), file.toPath()))
                .isEqualTo(file.getName() + " " + file.getName() + " Hello");
        assertThat(client.postMultipartImplicit(file.getName(), contents)).isEqualTo(file.getName() + " file Hello");
        assertThat(client.postMultipartImplicit(file.getName(), Buffer.buffer(contents)))
                .isEqualTo(file.getName() + " file Hello");
        assertThat(client.postMultipartImplicit(file.getName(), Multi.createFrom().items(contentsForMulti)))
                .isEqualTo(file.getName() + " file Hello");
        assertThat(client.postMultipartEntityImplicit(file.getName(), person))
                .isEqualTo(file.getName() + " Stef:Epardaud");

        assertThat(client.postMultipartImplicitFileUpload("Foo", new FileUpload() {
            @Override
            public String name() {
                return "file";
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
                return -1;
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
        }))
                .isEqualTo("Foo " + file.getName() + " Hello");
    }

    @Path("form")
    @ApplicationScoped
    public static class Resource {
        @Path("multipart")
        @POST
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        public String uploadMultipart(@RestForm String name, @RestForm FileUpload file) throws IOException {
            return name + " " + file.fileName() + " " + Files.readString(file.filePath());
        }

        @Path("multipart-entity")
        @POST
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        public String uploadMultipart(@RestForm String name, @PartType(MediaType.APPLICATION_JSON) @RestForm Person entity) {
            return name + " " + entity.firstName + ":" + entity.lastName;
        }

        @Path("urlencoded")
        @POST
        @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
        public String uploadMultipart(@RestForm String name) {
            return name;
        }
    }

    @Path("form")
    @RegisterProvider(TestJacksonBasicMessageBodyReader.class)
    @RegisterProvider(TestJacksonBasicMessageBodyWriter.class)
    public interface Client {
        @Path("multipart")
        @POST
        String postMultipartImplicit(@RestForm String name, @RestForm File file);

        @Path("multipart")
        @POST
        String postMultipartImplicit(@RestForm String name, @RestForm java.nio.file.Path file);

        @Path("multipart")
        @POST
        String postMultipartImplicit(@RestForm String name, @RestForm byte[] file);

        @Path("multipart")
        @POST
        String postMultipartImplicit(@RestForm String name, @RestForm Multi<Byte> file);

        @Path("multipart")
        @POST
        String postMultipartImplicit(@RestForm String name, @RestForm Buffer file);

        @Path("multipart-entity")
        @POST
        String postMultipartEntityImplicit(@RestForm String name,
                @PartType(MediaType.APPLICATION_JSON) @RestForm Person entity);

        @Path("multipart")
        @POST
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        String postMultipartExplicit(@RestForm String name, @RestForm File file);

        @Path("multipart")
        @POST
        String postMultipartImplicitFileUpload(@RestForm String name, @RestForm FileUpload file);

        @Path("urlencoded")
        @POST
        String postUrlencodedImplicit(@RestForm String name);

        @Path("urlencoded")
        @POST
        @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
        String postUrlencodedExplicit(@RestForm String name);
    }

    public static class Person {
        public String firstName;
        public String lastName;
    }
}
