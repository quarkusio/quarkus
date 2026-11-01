package io.quarkus.devjsonrpc.deployment;

import io.quarkus.builder.item.EmptyBuildItem;

/**
 * Marker that the {@link io.quarkus.devjsonrpc.runtime.comms.JsonRpcRouter} is fully initialized (endpoints and codec).
 * <p>
 * A {@code RUNTIME_INIT} step that registers an HTTP route using the router should consume this, so the route is only
 * served once the router is ready. On a dev mode restart the HTTP server is already accepting connections, so without
 * this ordering a client can reach a router that is still being initialized.
 */
public final class JsonRpcRouterBuildItem extends EmptyBuildItem {
}
