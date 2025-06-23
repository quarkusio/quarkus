package io.quarkus;

import jakarta.inject.Singleton;

import io.micrometer.core.instrument.Tags;
import io.quarkus.micrometer.runtime.HttpServerMetricsTagsContributor;

@Singleton
public class ResponseHeaderTag implements HttpServerMetricsTagsContributor {

    @Override
    public Tags contribute(Context context) {
        var headerValue = context.response().headers().get("foo-response");
        String value = "UNSET";
        if ((headerValue != null) && !headerValue.isEmpty()) {
            value = headerValue;
        }
        return Tags.of("foo-response", value);
    }
}
