package io.quarkus.rest.client.reactive.multipart;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.EntityPart;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class EntityPartClientTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(EchoResource.class, EntityPartWriterResource.class,
                                    EchoFormResource.class,
                                    EntityPartContainerClient.class, ClientFormWithEntityPart.class,
                                    Greeting.class, EntityPartListClient.class,
                                    FileManagerClient.class, EntityPartListFilter.class);
                }
            });

    @TestHTTPResource
    URI baseUri;

    @Test
    void sendAndReceiveEntityParts() throws IOException {
        List<EntityPart> parts = List.of(
                EntityPart.withName("greeting")
                        .content("hello")
                        .mediaType(MediaType.TEXT_PLAIN_TYPE)
                        .build(),
                EntityPart.withName("count")
                        .content("42")
                        .mediaType(MediaType.TEXT_PLAIN_TYPE)
                        .build());

        Client client = ClientBuilder.newClient();
        try {
            Response response = client.target(baseUri).path("/echo-parts")
                    .request(MediaType.TEXT_PLAIN)
                    .post(Entity.entity(parts, MediaType.MULTIPART_FORM_DATA_TYPE));

            assertThat(response.getStatus()).isEqualTo(200);
            String body = response.readEntity(String.class);
            assertThat(body).contains("greeting=hello");
            assertThat(body).contains("count=42");
        } finally {
            client.close();
        }
    }

    @Test
    void entityPartBuilderUsesMessageBodyWriter() throws IOException {
        Client client = ClientBuilder.newClient();
        try {
            Response response = client.target(baseUri).path("/entity-part-writer")
                    .request(MediaType.TEXT_PLAIN)
                    .get();

            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.readEntity(String.class)).isEqualTo("{\"message\":\"hello\"}");
        } finally {
            client.close();
        }
    }

    @Test
    void entityPartInClientContainer() throws IOException {
        EntityPartContainerClient restClient = RestClientBuilder.newBuilder()
                .baseUri(baseUri).build(EntityPartContainerClient.class);

        ClientFormWithEntityPart form = new ClientFormWithEntityPart();
        form.dataPart = EntityPart.withName("dataPart")
                .content("some data")
                .mediaType(MediaType.TEXT_PLAIN_TYPE)
                .build();
        form.name = "testName";

        String result = restClient.send(form);
        assertThat(result).contains("dataPart=some data");
        assertThat(result).contains("name=testName");
    }

    @Test
    void entityPartListAsRestClientBody() throws IOException {
        EntityPartListClient restClient = RestClientBuilder.newBuilder()
                .baseUri(baseUri).build(EntityPartListClient.class);

        List<EntityPart> parts = List.of(
                EntityPart.withName("greeting")
                        .content("hello")
                        .mediaType(MediaType.TEXT_PLAIN_TYPE)
                        .build(),
                EntityPart.withName("count")
                        .content("42")
                        .mediaType(MediaType.TEXT_PLAIN_TYPE)
                        .build());

        String result = restClient.echo(parts);
        assertThat(result).contains("greeting=hello");
        assertThat(result).contains("count=42");
    }

    /**
     * Mirrors the TCK's {@code EntityPartTest}, which is excluded from our TCK run because it is
     * {@code @RunAsClient}.
     */
    @Test
    void entityPartListVisibleToClientRequestFilter() throws Exception {
        List<EntityPart> parts = List.of(
                EntityPart.withName("test-file1.txt")
                        .fileName("test-file1.txt")
                        .content("content1".getBytes(StandardCharsets.UTF_8))
                        .mediaType(MediaType.APPLICATION_OCTET_STREAM_TYPE)
                        .build(),
                EntityPart.withFileName("test-file2.txt")
                        .content(new ByteArrayInputStream("content2".getBytes(StandardCharsets.UTF_8)))
                        .mediaType(MediaType.APPLICATION_OCTET_STREAM_TYPE)
                        .build());

        try (FileManagerClient restClient = RestClientBuilder.newBuilder()
                .baseUri(baseUri)
                .register(EntityPartListFilter.class)
                .build(FileManagerClient.class)) {
            Response response = restClient.uploadFile(parts);

            assertThat(response.getStatus()).isEqualTo(201);
            assertThat(response.readEntity(String.class)).isEqualTo(
                    "test-file1.txt/test-file1.txt=content1;test-file2.txt/test-file2.txt=content2;");
        }
    }

    @Path("/echo-parts")
    public static class EchoResource {

        @POST
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        @Produces(MediaType.TEXT_PLAIN)
        public String echo(List<EntityPart> parts) throws IOException {
            List<String> results = new ArrayList<>();
            for (EntityPart part : parts) {
                String content = new String(part.getContent().readAllBytes(), StandardCharsets.UTF_8);
                results.add(part.getName() + "=" + content);
            }
            return String.join(",", results);
        }
    }

    @Path("/entity-part-writer")
    public static class EntityPartWriterResource {

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public String buildEntityPart() throws IOException {
            Greeting g = new Greeting();
            g.message = "hello";
            EntityPart part = EntityPart.withName("data")
                    .mediaType(MediaType.APPLICATION_JSON_TYPE)
                    .content(g, Greeting.class)
                    .build();
            return new String(part.getContent().readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Path("/echo-form")
    public static class EchoFormResource {

        @POST
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        @Produces(MediaType.TEXT_PLAIN)
        public String echo(@RestForm EntityPart dataPart, @RestForm String name) throws IOException {
            String content = new String(dataPart.getContent().readAllBytes(), StandardCharsets.UTF_8);
            return "dataPart=" + content + ",name=" + name;
        }
    }

    @Path("/echo-form")
    public interface EntityPartContainerClient {
        @POST
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        @Produces(MediaType.TEXT_PLAIN)
        String send(ClientFormWithEntityPart form);
    }

    @Path("/echo-parts")
    public interface EntityPartListClient {
        @POST
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        @Produces(MediaType.TEXT_PLAIN)
        String echo(List<EntityPart> parts);
    }

    @Path("/files")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.MULTIPART_FORM_DATA)
    public interface FileManagerClient extends AutoCloseable {
        @POST
        @Path("upload")
        Response uploadFile(List<EntityPart> entityParts) throws IOException;
    }

    public static class EntityPartListFilter implements ClientRequestFilter {
        @Override
        @SuppressWarnings("unchecked")
        public void filter(ClientRequestContext requestContext) throws IOException {
            List<EntityPart> parts = (List<EntityPart>) requestContext.getEntity();
            StringBuilder body = new StringBuilder();
            for (EntityPart part : parts) {
                body.append(part.getName())
                        .append('/')
                        .append(part.getFileName().orElse(null))
                        .append('=')
                        .append(part.getContent(String.class))
                        .append(';');
            }
            requestContext.abortWith(Response.status(201)
                    .entity(body.toString())
                    .type(MediaType.TEXT_PLAIN_TYPE)
                    .build());
        }
    }

    public static class ClientFormWithEntityPart {
        @RestForm
        public EntityPart dataPart;

        @RestForm
        public String name;
    }

    public static class Greeting {
        public String message;
    }
}
