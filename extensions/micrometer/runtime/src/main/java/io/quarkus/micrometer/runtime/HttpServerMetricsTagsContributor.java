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
    }
}
