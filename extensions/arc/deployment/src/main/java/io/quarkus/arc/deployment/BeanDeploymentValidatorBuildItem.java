package io.quarkus.arc.deployment;

import io.quarkus.arc.processor.BeanDeploymentValidator;
import io.quarkus.builder.item.MultiBuildItem;

/**
 * Register a custom {@link BeanDeploymentValidator} which can either perform additional validation or enforce
 * validation skip for certain components.
 * 
 * This build item will be removed at some point post Quarkus 1.11.
 * 
 * @deprecated Use {@link ValidationPhaseBuildItem} instead
 */
@Deprecated
public final class BeanDeploymentValidatorBuildItem extends MultiBuildItem {

    private final BeanDeploymentValidator beanDeploymentValidator;

    public BeanDeploymentValidatorBuildItem(BeanDeploymentValidator beanDeploymentValidator) {
        this.beanDeploymentValidator = beanDeploymentValidator;
    }

    public BeanDeploymentValidator getBeanDeploymentValidator() {
        return beanDeploymentValidator;
    }

}
