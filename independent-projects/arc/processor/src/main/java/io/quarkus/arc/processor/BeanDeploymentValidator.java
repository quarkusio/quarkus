package io.quarkus.arc.processor;

import java.util.Collection;
import java.util.List;

import jakarta.enterprise.inject.spi.DeploymentException;

/**
 * Makes it possible to validate the bean deployment and also to skip some validation rules for specific components.
 */
public interface BeanDeploymentValidator extends BuildExtension {

    /**
     * At this point, all beans/observers are registered. This method should call
     * {@link ValidationContext#addDeploymentProblem(Throwable)} if validation fails.
     *
     * @see Key#INJECTION_POINTS
     * @see Key#BEANS
     * @see Key#OBSERVERS
     * @see DeploymentException
     */
    default void validate(ValidationContext context) {
    }

    interface ValidationContext extends BuildContext {

        void addDeploymentProblem(Throwable t);

        List<Throwable> getDeploymentProblems();

        /**
         *
         * @return a new stream of beans that form the deployment
         */
        BeanStream beans();

        /**
         *
         * @return a new stream of beans that are considered {@code unused} and were removed from the deployment
         */
        BeanStream removedBeans();

        default Collection<InjectionPointInfo> getInjectionPoints() {
            return get(BuildExtension.Key.INJECTION_POINTS);
        }

    }

}
