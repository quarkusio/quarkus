package org.jboss.resteasy.reactive.server.vertx.test.stream;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.vertx.core.buffer.Buffer;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.Date;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.jboss.resteasy.reactive.common.util.MultiCollectors;
import org.reactivestreams.Publisher;

@Path("stream")
public class StreamResource {

    private static final int INITIAL_BUFFER_SIZE = 2048;

    @Path("text/collect")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<String> getCollectedText() {
        return MultiCollectors.concatenateStrings(getStreamedText());
    }

    @Path("text/stream")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Multi<String> getStreamedText() {
        return Multi.createFrom().items("foo", "bar");
    }

    @Path("text/stream/publisher")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Publisher<String> getStreamedTextPublisher() {
        return Multi.createFrom().items("foo", "bar");
    }

    @Path("byte-arrays/collect")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<byte[]> getCollectedByteArrays() {
        return MultiCollectors.concatenateByteArrays(getStreamedByteArrays());
    }

    @Path("byte-arrays/stream")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Multi<byte[]> getStreamedByteArrays() {
        return Multi.createFrom().items("foo".getBytes(), "bar".getBytes());
    }

    @Path("char-arrays/collect")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<char[]> getCollectedCharacterArrays() {
        return MultiCollectors.concatenateCharArrays(getStreamedCharArrays());
    }

    @Path("char-arrays/stream")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Multi<char[]> getStreamedCharArrays() {
        return Multi.createFrom().items("foo".toCharArray(), "bar".toCharArray());
    }

    @Path("char-arrays/stream/publisher")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Publisher<char[]> getStreamedCharArraysPublisher() {
        return Multi.createFrom().items("foo".toCharArray(), "bar".toCharArray());
    }

    @Path("buffer/collect")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<Buffer> getCollectedBuffers() {
        return concatenateBuffers(getStreamedBuffers());
    }

    @Path("buffer/stream")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Multi<Buffer> getStreamedBuffers() {
        return Multi.createFrom().items(Buffer.buffer("foo"), Buffer.buffer("bar"));
    }

    public static Uni<Buffer> concatenateBuffers(Multi<Buffer> multi) {
        return multi.collect().in(() -> Buffer.buffer(INITIAL_BUFFER_SIZE),
                (accumulatingBuffer, receivedBuffer) -> accumulatingBuffer.appendBuffer(receivedBuffer));
    }

    private boolean receivedCancel = false;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("infinite/stream")
    public Multi<String> infiniteStream() {
        receivedCancel = false;
        return Multi.createFrom().emitter(emitter -> {
            ScheduledExecutorService scheduler = Infrastructure.getDefaultWorkerPool();
            // this should never complete, but let's kill it after 30 seconds
            ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(() -> {
                String str = "Called at " + new Date();
                emitter.emit(str);
            }, 0, 1, TimeUnit.SECONDS);

            // catch client close
            emitter.onTermination(() -> {
                if (emitter.isCancelled()) {
                    receivedCancel = true;
                    if (!future.isCancelled())
                        future.cancel(true);
                }
            });

            // die in 30s max
            scheduler.schedule(() -> {
                if (!future.isCancelled()) {
                    future.cancel(true);
                    // just in case
                    emitter.complete();
                }
            }, 30, TimeUnit.SECONDS);
        });
    }

    @GET
    @Path("infinite/stream-was-cancelled")
    public String infiniteStreamWasCancelled() {
        return receivedCancel ? "OK" : "KO";
    }

    @Path("sse")
    @GET
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public Multi<String> sse() {
        return Multi.createFrom().items("a", "b", "c");
    }

    @Path("sse/throw")
    @GET
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public Multi<String> sseThrows() {
        throw new IllegalStateException("STOP");
    }
}
