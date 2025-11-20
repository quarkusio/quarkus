package org.jboss.resteasy.reactive.client.impl;

import java.util.function.Consumer;

import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;

public interface VertxRequestCustomizingClientBuilder<C extends VertxRequestCustomizingClientBuilder> {

    /**
     * Specifies the HTTP client options to use.
     *
     * @param httpClientOptionsClass the HTTP client options to use.
     * @return the current builder
     */
    C httpClientOptions(Class<? extends HttpClientOptions> httpClientOptionsClass);

    /**
     * Specifies the HTTP client options to use.
     *
     * @param httpClientOptions the HTTP client options to use.
     * @return the current builder
     */
    C httpClientOptions(HttpClientOptions httpClientOptions);

    /**
     * Specifies a callback which will be invoked after Quarkus has populated {@link HttpClientOptions} but before Vert.x uses
     * it to create {@link io.vertx.core.http.HttpClient}
     */
    C httpClientOptionsCustomizer(Consumer<HttpClientOptions> httpClientOptionsCustomizer);

    /**
     * Specifies a callback which will be invoked when Vert.x has created the {@link io.vertx.core.http.HttpClientRequest}
     * and before Quarkus does anything with it.
     */
    C httpClientRequestCustomizer(Consumer<HttpClientRequest> httpClientOptionsCustomizer);
}
