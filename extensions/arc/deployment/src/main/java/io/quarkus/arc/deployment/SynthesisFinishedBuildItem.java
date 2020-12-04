package io.quarkus.arc.deployment;

import io.quarkus.arc.processor.BeanDeployment;

/**
 * Consumers of this build item can easily inspect all beans, observers and injection points registered in the
 * application. Synthetic beans and observers are included. If interested in class-based components only you can use the
 * {@link BeanDiscoveryFinishedBuildItem} instead.
 * <p>
 * Additionaly, the bean resolver can be used to apply the type-safe resolution rules, e.g. to find out whether there is a bean
 * that would satisfy certain combination of required type and qualifiers.
 * 
 * @see BeanDiscoveryFinishedBuildItem
 */
public final class SynthesisFinishedBuildItem extends RegisteredComponentsBuildItem {

    public SynthesisFinishedBuildItem(BeanDeployment beanDeployment) {
        super(beanDeployment);
    }

}
