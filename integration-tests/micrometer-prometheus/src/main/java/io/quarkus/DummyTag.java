package io.quarkus;

import jakarta.inject.Singleton;

import io.micrometer.core.instrument.Tags;
import io.quarkus.micrometer.runtime.HttpServerMetricsTagsContributor;

@Singleton
public class DummyTag implements HttpServerMetricsTagsContributor {

    @Override
    public Tags contribute(Context context) {
        return Tags.of("dummy", "value");
    }
}
