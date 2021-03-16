package io.quarkus.arc.deployment;

import io.quarkus.arc.processor.BeanDeployment;

/**
 * Consumers of this build item can easily inspect all class-based beans, observers and injection points registered in the
 * application. Synthetic beans and observers are not included. If you need to consider synthetic components as well use
 * the {@link SynthesisFinishedBuildItem} instead.
 * <p>
 * Additionaly, the bean resolver can be used to apply the type-safe resolution rules, e.g. to find out wheter there is a bean
 * that would satisfy certain combination of required type and qualifiers.
 * 
 * @see SynthesisFinishedBuildItem
 */
public final class BeanDiscoveryFinishedBuildItem extends RegisteredComponentsBuildItem {

    public BeanDiscoveryFinishedBuildItem(BeanDeployment beanDeployment) {
        super(beanDeployment);
    }

}
