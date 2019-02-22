package io.quarkus.arc.deployment;

import org.jboss.builder.item.MultiBuildItem;
import org.jboss.quarkus.arc.processor.BeanDeploymentValidator;

public final class BeanDeploymentValidatorBuildItem extends MultiBuildItem {

    private final BeanDeploymentValidator beanDeploymentValidator;

    public BeanDeploymentValidatorBuildItem(BeanDeploymentValidator beanDeploymentValidator) {
        this.beanDeploymentValidator = beanDeploymentValidator;
    }

    public BeanDeploymentValidator getBeanDeploymentValidator() {
        return beanDeploymentValidator;
    }

}
