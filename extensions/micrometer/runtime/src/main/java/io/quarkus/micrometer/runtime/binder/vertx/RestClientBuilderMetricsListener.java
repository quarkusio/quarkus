package io.quarkus.micrometer.runtime.binder.vertx;

import java.util.function.Consumer;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.spi.RestClientBuilderListener;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.client.impl.VertxRequestCustomizingClientBuilder;

import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.impl.HttpClientRequestInternal;

public class RestClientBuilderMetricsListener implements RestClientBuilderListener {

    private static final Logger log = Logger.getLogger(VertxMeterBinderAdapter.class);

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public void onNewBuilder(RestClientBuilder builder) {
        if ("io.quarkus.restclient.runtime.QuarkusRestClientBuilder".equals(builder.getClass().getName())) {
            // the customization won't work and is not necessary for RESTEasy client
            return;
        }
        if (builder instanceof VertxRequestCustomizingClientBuilder vertxRequestCustomizingClientBuilder) {
            // ensure that we don't have duplicate metrics because the Vert.x HTTP Client also creates metrics
            vertxRequestCustomizingClientBuilder.httpClientRequestCustomizer(new Consumer<HttpClientRequest>() {
                @Override
                public void accept(HttpClientRequest httpClientRequest) {
                    if (httpClientRequest instanceof HttpClientRequestInternal httpClientRequestInternal) {
                        Object metricObj = httpClientRequestInternal.metric();
                        if (metricObj instanceof IgnorableMetric metric) {
                            // TODO we need to introduce a handler here as we don't know the type at this point
                            metric.markAsIgnored();
                        }
                    } else {
                        log.warnf("Unhandled HttpClientRequest type: '%s'. Metrics might not work properly.",
                                httpClientRequest.getClass().getName());
                    }
                }
            });
        }

    }
}
