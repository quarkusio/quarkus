package io.quarkus.arc.deployment;

import io.quarkus.builder.item.EmptyBuildItem;

/**
 * This build item should be consumed by build steps that require RUNTIME_INIT synthetic beans to be initialized.
 * 
 * @see SyntheticBeanBuildItem
 */
public final class SyntheticBeansRuntimeInitBuildItem extends EmptyBuildItem {

}
