package io.quarkus.rest.client.reactive;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URI;
import java.time.Duration;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.client.reactive.redirect.RedirectingResource;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.smallrye.mutiny.Uni;

public class FileDownloadTest {

    private static final long FIFTY_MEGA = 50 * 1024L * 1024L;

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Client.class, RedirectingResource.class))
            .overrideRuntimeConfigKey("quarkus.rest-client.follow-redirects", "true");

    @TestHTTPResource
    URI uri;

    @Test
    void test() {
        Client client = RestClientBuilder.newBuilder().baseUri(uri).build(Client.class);
        File file = client.file();
        assertThat(file).exists().hasSize(FIFTY_MEGA);

        java.nio.file.Path path = client.path();
        assertThat(path).exists().hasSize(FIFTY_MEGA);

        file = client.uniFile().await().atMost(Duration.ofSeconds(10));
        assertThat(file).exists().hasSize(FIFTY_MEGA);

        path = client.uniPath().await().atMost(Duration.ofSeconds(10));
        assertThat(path).exists().hasSize(FIFTY_MEGA);
    }

    @Path("/test")
    public interface Client {
        @GET
        @Path("file")
        File file();

        @GET
        @Path("file")
        java.nio.file.Path path();

        @GET
        @Path("file")
        Uni<File> uniFile();

        @GET
        @Path("file")
        Uni<java.nio.file.Path> uniPath();
    }

    @Path("test")
    public static class Resource {

        @Path("file")
        @GET
        public File file() throws IOException {
            File file = createTempFileToDownload();
            RandomAccessFile f = new RandomAccessFile(file, "rw");
            f.setLength(FIFTY_MEGA);
            return file;
        }

        private static File createTempFileToDownload() throws IOException {
            File file = File.createTempFile("toDownload", ".txt");
            file.deleteOnExit();
            return file;
        }
    }
}
