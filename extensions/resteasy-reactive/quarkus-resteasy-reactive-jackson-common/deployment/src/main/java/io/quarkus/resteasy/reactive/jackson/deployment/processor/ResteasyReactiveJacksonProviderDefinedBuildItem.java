package io.quarkus.resteasy.reactive.jackson.deployment.processor;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A BuildItem to mark that the server side jackson provider is defined.
 *
 * @Deprecated because now the rest client reactive will always add its own jackson provider.
 */
@Deprecated(forRemoval = true)
public final class ResteasyReactiveJacksonProviderDefinedBuildItem extends MultiBuildItem {

}
