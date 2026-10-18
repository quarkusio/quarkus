package io.quarkus.micrometer.test;

import jakarta.inject.Singleton;

import io.micrometer.core.instrument.Tags;
import io.quarkus.micrometer.runtime.HttpClientMetricsTagsContributor;

@Singleton
public class ClientDummyTag implements HttpClientMetricsTagsContributor {

    @Override
    public Tags contribute(Context context) {
        return Tags.of("dummy", "value");
    }
}
