package io.quarkus.it.vertx;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.netty.util.internal.CleanableDirectBuffer;
import io.netty.util.internal.PlatformDependent;

@Path("/netty-direct-buffer")
public class NettyDirectBufferResource {
    @GET
    @Path("/cleaner")
    @Produces(MediaType.TEXT_PLAIN)
    public String cleaner() {
        CleanableDirectBuffer allocation = PlatformDependent.allocateDirect(1);
        try {
            return allocation.getClass().getName();
        } finally {
            allocation.clean();
        }
    }

    @GET
    @Path("/empty")
    @Produces(MediaType.TEXT_PLAIN)
    public String empty() {
        CleanableDirectBuffer allocation = PlatformDependent.allocateDirect(0);
        try {
            return allocation.buffer().isDirect() + ":" + allocation.buffer().capacity();
        } finally {
            allocation.clean();
        }
    }

    @GET
    @Path("/resize/{capacity}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public byte[] resize(@PathParam("capacity") int capacity) {
        CleanableDirectBuffer allocation = PlatformDependent.allocateDirect(64);
        try {
            for (int i = 0; i < 64; i++) {
                allocation.buffer().put(i, (byte) i);
            }
            allocation = PlatformDependent.reallocateDirect(allocation, capacity);
            if (allocation.buffer().capacity() != capacity) {
                throw new IllegalStateException("Unexpected capacity after reallocation");
            }
            byte[] contents = new byte[Math.min(64, capacity)];
            allocation.buffer().get(contents);
            return contents;
        } finally {
            allocation.clean();
        }
    }

    @GET
    @Path("/cross-thread")
    @Produces(MediaType.TEXT_PLAIN)
    public String crossThread() throws ExecutionException, InterruptedException {
        CleanableDirectBuffer allocation = PlatformDependent.allocateDirect(64);
        allocation.buffer().putLong(0, 42);
        return CompletableFuture.supplyAsync(() -> {
            try {
                return Long.toString(allocation.buffer().getLong(0));
            } finally {
                allocation.clean();
            }
        }).get();
    }
}
