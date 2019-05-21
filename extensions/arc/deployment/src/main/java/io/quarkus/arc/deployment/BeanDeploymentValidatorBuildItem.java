package io.quarkus.arc.deployment;

import io.quarkus.arc.processor.BeanDeploymentValidator;
import io.quarkus.builder.item.MultiBuildItem;

public final class BeanDeploymentValidatorBuildItem extends MultiBuildItem {

    private final BeanDeploymentValidator beanDeploymentValidator;

    public BeanDeploymentValidatorBuildItem(BeanDeploymentValidator beanDeploymentValidator) {
        this.beanDeploymentValidator = beanDeploymentValidator;
    }

    public BeanDeploymentValidator getBeanDeploymentValidator() {
        return beanDeploymentValidator;
    }

}
