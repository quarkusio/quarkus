package io.quarkus.micrometer.runtime;

import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.config.MeterFilter;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.spi.observability.HttpResponse;

/**
 * Allows code to add additional Micrometer {@link Tags} to the metrics collected for completed HTTP server requests.
 * <p>
 * The implementations of this interface are meant to be registered via CDI beans.
 *
 * @see MeterFilter for a more advanced and feature complete way of interacting with {@link Tags}
 */
public interface HttpServerMetricsTagsContributor {

    /**
     * Called when Vert.x http server response has ended
     */
    Tags contribute(Context context);

    interface Context {
        HttpServerRequest request();

        HttpResponse response();

        /**
         * Gives access to the contextual data that was added while the HTTP request was active.
         * This can be found of doing something like {@link io.smallrye.common.vertx.ContextLocals#get(String)},
         * however this method is needed because {@link io.smallrye.common.vertx.ContextLocals#get(String)} won't
         * work when {@link HttpServerMetricsTagsContributor#contribute(Context)} is called as the HTTP request has
         * already gone away.
         * <p>
         * Beware of high cardinality causing memory usage increase.
         * <p>
         * Don't use keys that might hold many different values (more than a few dozens).
         * The problem is not just about the current request but the combination of different values from all the
         * requests the server will receive.
         */
        <T> T requestContextLocalData(Object key);
    }
}
