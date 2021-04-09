package io.quarkus.arc.deployment.staticmethods;

import io.quarkus.builder.item.EmptyBuildItem;

/**
 * Marker build item to signal that bytecode transformers used for static method interception were registered.
 * <p>
 * ASM class visitors produced by transformers registered by consumers of this build item will be run before visitors used for
 * static method interception.
 */
public final class InterceptedStaticMethodsTransformersRegisteredBuildItem extends EmptyBuildItem {

}
