package io.quarkus.rest.client.reactive.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.ext.WriterInterceptor;
import jakarta.ws.rs.ext.WriterInterceptorContext;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class EntityStreamRequestFilterTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar.addClasses(Resource.class, Client.class, GzipRequestFilter.class,
                    EntityStreamReadingFilter.class, HeaderAddingWriterInterceptor.class));

    @TestHTTPResource
    URI baseUri;

    @Test
    void entityStreamReplacedByRequestFilterIsUsedToWriteTheEntity() {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri).register(GzipRequestFilter.class)
                .build(Client.class);
        assertThat(client.gzip("hello, compressed world")).isEqualTo("gzip:hello, compressed world");
    }

    @Test
    void entityStreamReplacedByRequestFilterIsUsedWhenWriterInterceptorsAreRegistered() {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri).register(GzipRequestFilter.class)
                .register(HeaderAddingWriterInterceptor.class)
                .build(Client.class);
        assertThat(client.gzip("hello, intercepted world")).isEqualTo("gzip:hello, intercepted world:intercepted");
    }

    @Test
    void requestFilterCanObtainTheEntityStreamWithoutReplacingIt() {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri).register(EntityStreamReadingFilter.class)
                .build(Client.class);
        assertThat(client.echo("hello")).isEqualTo("hello");
    }

    @Test
    void replacingTheEntityStreamOfAFileUploadIsRejected() throws IOException {
        File file = Files.createTempFile("entity-stream", ".txt").toFile();
        file.deleteOnExit();
        Files.writeString(file.toPath(), "file content");
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri).register(GzipRequestFilter.class)
                .build(Client.class);
        assertThatThrownBy(() -> client.gzipFile(file))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("entity stream");
    }

    @Path("/")
    public static class Resource {

        @POST
        @Path("/gzip")
        public String gunzip(byte[] body) throws IOException {
            try (GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(body))) {
                return "gzip:" + new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        }

        @POST
        @Path("/echo")
        public String echo(String body) {
            return body;
        }
    }

    public interface Client {

        @POST
        @Path("/gzip")
        String gzip(String body);

        @POST
        @Path("/gzip")
        String gzipFile(File body);

        @POST
        @Path("/echo")
        String echo(String body);
    }

    public static class GzipRequestFilter implements ClientRequestFilter {

        @Override
        public void filter(ClientRequestContext context) throws IOException {
            context.setEntityStream(new GZIPOutputStream(context.getEntityStream()));
            context.getHeaders().putSingle("Content-Encoding", "gzip");
        }
    }

    public static class EntityStreamReadingFilter implements ClientRequestFilter {

        @Override
        public void filter(ClientRequestContext context) {
            assertThat(context.getEntityStream()).isNotNull();
        }
    }

    public static class HeaderAddingWriterInterceptor implements WriterInterceptor {

        @Override
        public void aroundWriteTo(WriterInterceptorContext context) throws IOException, WebApplicationException {
            context.setEntity(context.getEntity() + ":intercepted");
            context.proceed();
        }
    }
}
