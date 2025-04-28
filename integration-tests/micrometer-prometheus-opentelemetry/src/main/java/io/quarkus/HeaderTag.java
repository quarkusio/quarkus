package io.quarkus;

import jakarta.inject.Singleton;

import io.micrometer.core.instrument.Tags;
import io.quarkus.micrometer.runtime.HttpServerMetricsTagsContributor;

@Singleton
public class HeaderTag implements HttpServerMetricsTagsContributor {

    @Override
    public Tags contribute(Context context) {
        String headerValue = context.request().getHeader("Foo");
        String value = "UNSET";
        if ((headerValue != null) && !headerValue.isEmpty()) {
            value = headerValue;
        }
        return Tags.of("foo", value);
    }
}
