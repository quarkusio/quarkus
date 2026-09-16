package io.quarkus.rest.client.reactive;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.smallrye.mutiny.Multi;

public class SendMultiBytesTest {

    private static final int CHUNK_SIZE = 64 * 1024;
    // 2 GB, larger than the heap of the test JVM: the body can only be sent without buffering it
    private static final int CHUNKS = 32 * 1024;
    private static final long TOTAL = (long) CHUNK_SIZE * CHUNKS;

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .overrideConfigKey("quarkus.http.limits.max-body-size", "4G");

    @TestHTTPResource
    URI uri;

    @Test
    public void sendMultiOfByteArrays() {
        Client client = RestClientBuilder.newBuilder().baseUri(uri).build(Client.class);

        long result = client.countBytes(chunks());

        assertThat(result).isEqualTo(TOTAL);
    }

    @Test
    public void pipeDownloadIntoUpload() {
        Client client = RestClientBuilder.newBuilder().baseUri(uri).build(Client.class);

        long result = client.countBytes(client.download());

        assertThat(result).isEqualTo(TOTAL);
    }

    @Test
    public void failingBodyFailsTheCall() {
        Client client = RestClientBuilder.newBuilder().baseUri(uri).build(Client.class);
        Multi<byte[]> body = Multi.createBy().concatenating().streams(
                chunks().select().first(100),
                Multi.createFrom().failure(new IOException("source broke")));

        assertThatThrownBy(() -> client.countBytes(body))
                .isInstanceOf(ProcessingException.class)
                .hasRootCauseMessage("source broke");
    }

    private static Multi<byte[]> chunks() {
        byte[] chunk = new byte[CHUNK_SIZE];
        Arrays.fill(chunk, (byte) 'x');
        return Multi.createFrom().range(0, CHUNKS).onItem().transform(i -> chunk);
    }

    @Path("test")
    public interface Client {

        @POST
        @Path("count")
        long countBytes(Multi<byte[]> body);

        @GET
        @Path("download")
        Multi<byte[]> download();
    }

    @Path("test")
    public static class Resource {

        @POST
        @Path("count")
        public long count(InputStream input) throws IOException {
            byte[] buf = new byte[CHUNK_SIZE];
            long total = 0;
            int n;
            while ((n = input.read(buf)) != -1) {
                total += n;
            }
            return total;
        }

        @GET
        @Path("download")
        @Produces(MediaType.APPLICATION_OCTET_STREAM)
        public Multi<byte[]> download() {
            return chunks();
        }
    }
}
