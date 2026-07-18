package io.quarkus;

import jakarta.inject.Singleton;

import io.micrometer.core.instrument.Tags;
import io.quarkus.micrometer.runtime.HttpServerMetricsTagsContributor;

@Singleton
public class ContextLocalTag implements HttpServerMetricsTagsContributor {

    @Override
    public Tags contribute(Context context) {
        String contextLocalData = context.requestContextLocalData("context-local");
        return Tags.of("dummy", contextLocalData != null ? contextLocalData : "value");
    }
}
