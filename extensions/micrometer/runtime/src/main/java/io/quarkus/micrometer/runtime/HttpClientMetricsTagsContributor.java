package io.quarkus.micrometer.runtime;

import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.config.MeterFilter;
import io.vertx.core.spi.observability.HttpRequest;

/**
 * Allows code to add additional Micrometer {@link Tags} to the metrics collected for completed HTTP client requests.
 * <p>
 * The implementations of this interface are meant to be registered via CDI beans.
 *
 * @see MeterFilter for a more advanced and feature complete way of interacting with {@link Tags}
 */
public interface HttpClientMetricsTagsContributor {

    /**
     * Called when Vert.x http client request has ended
     */
    Tags contribute(Context context);

    interface Context {
        HttpRequest request();
    }
}
