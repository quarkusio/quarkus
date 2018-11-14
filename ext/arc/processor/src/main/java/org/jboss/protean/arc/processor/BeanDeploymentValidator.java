package org.jboss.protean.arc.processor;

import javax.enterprise.inject.spi.DeploymentException;

/**
 * Makes it possible to validate the bean deployment.
 *
 * @author Martin Kouba
 */
public interface BeanDeploymentValidator extends BuildExtension {

    /**
     * At this point, all beans/observers are registered. This method should throw a {@link DeploymentException} if validation fails.
     *
     * @see Key#INJECTION_POINTS
     * @see Key#BEANS
     * @see Key#OBSERVERS
     * @see DeploymentException
     */
    void validate();

}
