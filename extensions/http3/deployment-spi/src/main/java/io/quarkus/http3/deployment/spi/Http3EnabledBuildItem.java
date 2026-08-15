package io.quarkus.http3.deployment.spi;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Marker build item indicating that HTTP/3 (QUIC) support is enabled.
 * <p>
 * Produced by the HTTP/3 extension's deployment processor.
 * Consumed optionally by {@code vertx-http} to configure HTTP/3 on the server.
 */
public final class Http3EnabledBuildItem extends SimpleBuildItem {

}
