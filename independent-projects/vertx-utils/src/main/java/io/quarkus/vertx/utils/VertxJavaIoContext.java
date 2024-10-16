package io.quarkus.vertx.utils;

import java.util.Optional;

import io.vertx.ext.web.RoutingContext;

/**
 * A context giving access to Vert.x {@link RoutingContext} and to some configuration values.
 */
public class VertxJavaIoContext {
    private final RoutingContext context;
    private final int minChunkSize;
    private final int outputBufferCapacity;

    public VertxJavaIoContext(RoutingContext context, int minChunkSize, int outputBufferSize) {
        this.context = context;
        this.minChunkSize = minChunkSize;
        this.outputBufferCapacity = outputBufferSize;
    }

    /**
     * @return the Vert.x routing context
     */
    public RoutingContext getRoutingContext() {
        return context;
    }

    /**
     * Returns the size of the chunks of memory allocated when writing data in bytes.
     *
     * @return the size of the chunks of memory allocated when writing data in bytes
     */
    public int getMinChunkSize() {
        return minChunkSize;
    }

    /**
     * Returns the capacity of the underlying response buffer in bytes. If a response is larger than this and no
     * content-length is provided then the request will be chunked.
     * <p>
     * Larger values may give slight performance increases for large responses, at the expense of more memory usage.
     *
     * @return the capacity of the underlying response buffer in bytes
     */
    public int getOutputBufferCapacity() {
        return outputBufferCapacity;
    }

    /**
     * You may want to override this method letting it return a non-empty {@link Optional} if your framework needs to
     * pass a user defined content length to the underlying {@link VertxOutputStream}.
     * <p>
     * The default implementation always returns an empty {@link Optional}.
     *
     * @return {@link Optional#empty()}
     */
    public Optional<String> getContentLength() {
        return Optional.empty();
    }

}
