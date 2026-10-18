package io.quarkus.micrometer.test;

import jakarta.inject.Singleton;

import io.micrometer.core.instrument.Tags;
import io.quarkus.micrometer.runtime.HttpClientMetricsTagsContributor;

@Singleton
public class ClientHeaderTag implements HttpClientMetricsTagsContributor {

    @Override
    public Tags contribute(Context context) {
        String headerValue = context.request().headers().get("Foo");
        String value = "UNSET";
        if ((headerValue != null) && !headerValue.isEmpty()) {
            value = headerValue;
        }
        return Tags.of("foo", value);
    }
}
