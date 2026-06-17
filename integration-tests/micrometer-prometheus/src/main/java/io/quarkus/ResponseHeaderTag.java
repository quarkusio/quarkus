package io.quarkus;

import java.util.Optional;

import jakarta.inject.Singleton;

import io.micrometer.core.instrument.Tags;
import io.quarkus.micrometer.runtime.HttpServerMetricsTagsContributor;
import io.vertx.core.spi.observability.HttpResponse;

@Singleton
public class ResponseHeaderTag implements HttpServerMetricsTagsContributor {

    @Override
    public Tags contribute(Context context) {
        Optional<HttpResponse> response = context.response();
        String value = "UNSET";
        if (response.isPresent()) {
            var headerValue = response.get().headers().get("foo-response");
            if ((headerValue != null) && !headerValue.isEmpty()) {
                value = headerValue;
            }
        }
        return Tags.of("foo-response", value);
    }
}
