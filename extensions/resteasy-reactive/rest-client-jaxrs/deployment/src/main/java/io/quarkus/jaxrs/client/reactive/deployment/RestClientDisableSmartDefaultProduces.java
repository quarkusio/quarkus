package io.quarkus.jaxrs.client.reactive.deployment;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * By default, RESTEasy Reactive uses text/plain content type for String values
 * and application/json for everything else.
 * MicroProfile Rest Client spec requires the implementations to always default to application/json.
 * This build item disables the "smart" behavior of RESTEasy Reactive to comply to the spec
 */
public final class RestClientDisableSmartDefaultProduces extends MultiBuildItem {
}
