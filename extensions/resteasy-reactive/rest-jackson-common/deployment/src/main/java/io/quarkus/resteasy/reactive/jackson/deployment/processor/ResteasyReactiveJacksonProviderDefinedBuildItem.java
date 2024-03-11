package io.quarkus.resteasy.reactive.jackson.deployment.processor;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A BuildItem to mark that the server side jackson provider is defined.
 * If not "emitted" by any of the processors, the reactive rest client (if used) will add its own jackson provider
 */
public final class ResteasyReactiveJacksonProviderDefinedBuildItem extends MultiBuildItem {

}
