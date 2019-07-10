package io.quarkus.arc.processor;

import java.util.List;
import javax.enterprise.inject.spi.DeploymentException;

/**
 * Makes it possible to validate the bean deployment and also to skip some validation rules for specific components.
 *
 * @author Martin Kouba
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
    default void validate(ValidationContext validationContext) {
    }

    /**
     * 
     * @param target
     * @param rule
     * @return {@code true} if the given validation rule should be skipped for the specified target
     */
    default boolean skipValidation(InjectionTargetInfo target, ValidationRule rule) {
        return false;
    }

    interface ValidationContext extends BuildContext {

        void addDeploymentProblem(Throwable t);

        List<Throwable> getDeploymentProblems();

    }

    public enum ValidationRule {

        NO_ARGS_CONSTRUCTOR;

        boolean skipFor(List<BeanDeploymentValidator> validators, InjectionTargetInfo target) {
            for (BeanDeploymentValidator validator : validators) {
                if (validator.skipValidation(target, this)) {
                    return true;
                }
            }
            return false;
        }

    }

}
