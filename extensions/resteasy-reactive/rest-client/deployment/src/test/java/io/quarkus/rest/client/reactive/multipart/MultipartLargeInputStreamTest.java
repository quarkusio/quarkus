package io.quarkus.rest.client.reactive.multipart;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.concurrent.TimeUnit;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.jboss.resteasy.reactive.PartType;
import org.jboss.resteasy.reactive.RestForm;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class MultipartLargeInputStreamTest {

    private static final long PART_SIZE = Runtime.getRuntime().maxMemory() + 256L * 1024 * 1024;

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(Resource.class, Client.class, ClientForm.class))
            .overrideConfigKey("quarkus.http.limits.max-body-size", "10G");

    @TestHTTPResource
    URI baseUri;

    @Test
    @Timeout(value = 5, unit = TimeUnit.MINUTES)
    void streamsPartLargerThanTheHeap() {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri).build(Client.class);
        ClientForm form = new ClientForm();
        form.file = new GeneratedInputStream(PART_SIZE);

        long received = client.upload(form);

        // the raw body also contains the multipart boundaries and part headers
        assertThat(received).isBetween(PART_SIZE, PART_SIZE + 1024);
    }

    /**
     * Produces {@code size} bytes without ever holding more than one buffer of them.
     */
    static class GeneratedInputStream extends InputStream {

        private final long size;
        private long position;

        GeneratedInputStream(long size) {
            this.size = size;
        }

        @Override
        public int read() {
            if (position >= size) {
                return -1;
            }
            return (int) (position++ & 0x7F);
        }

        @Override
        public int read(byte[] b, int off, int len) {
            if (position >= size) {
                return -1;
            }
            int count = (int) Math.min(len, size - position);
            for (int i = 0; i < count; i++) {
                b[off + i] = (byte) (position++ & 0x7F);
            }
            return count;
        }
    }

    @Path("/receive")
    public static class Resource {

        @POST
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        public long receive(InputStream body) throws IOException {
            byte[] buffer = new byte[64 * 1024];
            long count = 0;
            int read;
            while ((read = body.read(buffer)) != -1) {
                count += read;
            }
            return count;
        }
    }

    @Path("/receive")
    public interface Client {

        @POST
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        long upload(ClientForm form);
    }

    public static class ClientForm {

        @RestForm("file")
        @PartType(MediaType.APPLICATION_OCTET_STREAM)
        public InputStream file;
    }
}
