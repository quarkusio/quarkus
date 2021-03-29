package io.quarkus.micrometer.runtime.binder;

import javax.inject.Singleton;
import javax.ws.rs.Produces;

import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.internal.OnlyOnceLoggingDenyMeterFilter;
import io.quarkus.micrometer.runtime.config.runtime.HttpClientConfig;
import io.quarkus.micrometer.runtime.config.runtime.HttpServerConfig;

@Singleton
public class HttpMeterFilterProvider {

    HttpBinderConfiguration binderConfiguration;

    HttpMeterFilterProvider(HttpBinderConfiguration binderConfiguration) {
        this.binderConfiguration = binderConfiguration;
    }

    @Singleton
    @Produces
    public MeterFilter metricsHttpClientUriTagFilter(HttpClientConfig httpClientConfig) {
        if (binderConfiguration.isClientEnabled()) {
            return maximumAllowableUriTagsFilter(binderConfiguration.getHttpClientRequestsName(),
                    httpClientConfig.maxUriTags);
        }
        return null;
    }

    @Singleton
    @Produces
    public MeterFilter metricsHttpServerUriTagFilter(HttpServerConfig httpServerConfig) {
        if (binderConfiguration.isServerEnabled()) {
            return maximumAllowableUriTagsFilter(binderConfiguration.getHttpServerRequestsName(),
                    httpServerConfig.maxUriTags);
        }
        return null;
    }

    @Singleton
    @Produces
    public MeterFilter metricsHttpPushUriTagFilter(HttpServerConfig httpServerConfig) {
        if (binderConfiguration.isServerEnabled()) {
            return maximumAllowableUriTagsFilter(binderConfiguration.getHttpServerPushName(),
                    httpServerConfig.maxUriTags);
        }
        return null;
    }

    @Singleton
    @Produces
    public MeterFilter metricsHttpWebSocketsUriTagFilter(HttpServerConfig httpServerConfig) {
        if (binderConfiguration.isServerEnabled()) {
            return maximumAllowableUriTagsFilter(binderConfiguration.getHttpServerWebSocketConnectionsName(),
                    httpServerConfig.maxUriTags);
        }
        return null;
    }

    MeterFilter maximumAllowableUriTagsFilter(final String metricName, final int maximumTagValues) {
        MeterFilter denyFilter = new OnlyOnceLoggingDenyMeterFilter(() -> String
                .format("Reached the maximum number (%s) of URI tags for '%s'. Are you using path parameters?",
                        maximumTagValues, metricName));

        return MeterFilter.maximumAllowableTags(metricName, "uri", maximumTagValues,
                denyFilter);
    }
}
