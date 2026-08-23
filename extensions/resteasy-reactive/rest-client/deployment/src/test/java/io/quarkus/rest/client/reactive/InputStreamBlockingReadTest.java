package io.quarkus.rest.client.reactive;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import java.io.IOException;
import java.io.InputStream;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.WriterInterceptor;
import jakarta.ws.rs.ext.WriterInterceptorContext;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.BlockingOperationControl;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * An {@link InputStream} sent or received by the REST Client from a blocking context may perform blocking
 * operations when read, e.g. a database query producing the data: the client must never read such a stream on the
 * event loop.
 * See <a href="https://github.com/quarkusio/quarkus/issues/33346">GitHub issue #33346</a>.
 */
public class InputStreamBlockingReadTest {

    private static final int SIZE = 3 * 1024 * 1024;

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(Resource.class, Client.class, HeaderWriterInterceptor.class));

    @Test
    void uploadedStreamIsReadOnAWorkerThread() {
        given().get("/upload/plain").then().statusCode(200).body(equalTo("received:" + SIZE));
    }

    @Test
    void uploadedStreamIsReadOnAWorkerThreadWithWriterInterceptor() {
        given().get("/upload/intercepted").then().statusCode(200).body(equalTo("received:" + SIZE + ":intercepted"));
    }

    @Test
    void downloadedStreamCanBeConsumedWithBlockingWork() {
        given().get("/download/stream").then().statusCode(200).body(equalTo("read:" + SIZE));
    }

    @Test
    void downloadedResponseEntityStreamCanBeConsumedWithBlockingWork() {
        given().get("/download/response").then().statusCode(200).body(equalTo("read:" + SIZE));
    }

    @Path("/")
    public static class Resource {

        @POST
        @Path("data")
        @Consumes(MediaType.APPLICATION_OCTET_STREAM)
        public String receive(InputStream is, @jakarta.ws.rs.HeaderParam("X-Intercepted") String intercepted)
                throws IOException {
            long total = 0;
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) != -1) {
                total += n;
            }
            return "received:" + total + (intercepted != null ? ":" + intercepted : "");
        }

        @GET
        @Path("data")
        @Produces(MediaType.APPLICATION_OCTET_STREAM)
        public byte[] data() {
            return new byte[SIZE];
        }

        @GET
        @Path("upload/plain")
        public String uploadPlain(@Context UriInfo uriInfo) {
            Client client = RestClientBuilder.newBuilder().baseUri(uriInfo.getBaseUri()).build(Client.class);
            return client.upload(new BlockingInputStream(SIZE));
        }

        @GET
        @Path("upload/intercepted")
        public String uploadIntercepted(@Context UriInfo uriInfo) {
            Client client = RestClientBuilder.newBuilder().baseUri(uriInfo.getBaseUri())
                    .register(HeaderWriterInterceptor.class).build(Client.class);
            return client.upload(new BlockingInputStream(SIZE));
        }

        @GET
        @Path("download/stream")
        public String downloadStream(@Context UriInfo uriInfo) throws IOException {
            Client client = RestClientBuilder.newBuilder().baseUri(uriInfo.getBaseUri()).build(Client.class);
            try (InputStream is = client.download()) {
                return "read:" + drainWithBlockingWork(is);
            }
        }

        @GET
        @Path("download/response")
        public String downloadResponse(@Context UriInfo uriInfo) throws IOException {
            Client client = RestClientBuilder.newBuilder().baseUri(uriInfo.getBaseUri()).build(Client.class);
            try (Response response = client.downloadResponse();
                    InputStream is = response.readEntity(InputStream.class)) {
                return "read:" + drainWithBlockingWork(is);
            }
        }

        private static long drainWithBlockingWork(InputStream is) throws IOException {
            byte[] buf = new byte[8192];
            long total = 0;
            int n;
            while ((n = is.read(buf)) != -1) {
                total += n;
                blockingWork();
            }
            return total;
        }
    }

    /**
     * Simulates the blocking operation performed with each chunk, such as a database query.
     */
    static void blockingWork() {
        if (!BlockingOperationControl.isBlockingAllowed()) {
            throw new IllegalStateException("Blocking operation on thread " + Thread.currentThread().getName());
        }
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * A stream that produces its data through a blocking operation, the way a stream backed by a database would.
     */
    static class BlockingInputStream extends InputStream {

        private int remaining;

        BlockingInputStream(int size) {
            this.remaining = size;
        }

        @Override
        public int read() {
            if (remaining == 0) {
                return -1;
            }
            blockingWork();
            remaining--;
            return 0;
        }

        @Override
        public int read(byte[] b, int off, int len) {
            if (remaining == 0) {
                return -1;
            }
            blockingWork();
            int n = Math.min(len, remaining);
            remaining -= n;
            return n;
        }
    }

    public static class HeaderWriterInterceptor implements WriterInterceptor {

        @Override
        public void aroundWriteTo(WriterInterceptorContext context) throws IOException, WebApplicationException {
            context.getHeaders().putSingle("X-Intercepted", "intercepted");
            context.proceed();
        }
    }

    @Path("/data")
    public interface Client {

        @POST
        @Consumes(MediaType.APPLICATION_OCTET_STREAM)
        String upload(InputStream is);

        @GET
        InputStream download();

        @GET
        Response downloadResponse();
    }
}
