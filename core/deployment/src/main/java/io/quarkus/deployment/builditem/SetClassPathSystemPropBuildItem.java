package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A marker build item to make Quarkus set the {@code java.class.path} system property.
 * This system property is used in rare by libraries (Truffle for example) to create their own ClassLoaders.
 * The value of the system property is simply best effort, as there is no way to faithfully represent
 * the Quarkus ClassLoader hierarchies in a system property value.
 */
public final class SetClassPathSystemPropBuildItem extends MultiBuildItem {
}
