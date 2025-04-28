package io.quarkus;

import jakarta.inject.Singleton;

import io.micrometer.core.instrument.Tags;
import io.quarkus.micrometer.runtime.HttpServerMetricsTagsContributor;
import io.vertx.core.spi.observability.HttpResponse;

@Singleton
public class ResponseHeaderTag implements HttpServerMetricsTagsContributor {

    @Override
    public Tags contribute(Context context) {
        String value = "UNSET";
        HttpResponse response = context.response();
        // reset frames will not contain response
        if (response != null) {
            var headerValue = response.headers().get("foo-response");
            if ((headerValue != null) && !headerValue.isEmpty()) {
                value = headerValue;
            }
        }
        return Tags.of("foo-response", value);
    }
}
