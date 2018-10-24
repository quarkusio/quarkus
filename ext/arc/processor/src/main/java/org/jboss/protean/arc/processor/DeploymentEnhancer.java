package org.jboss.protean.arc.processor;

/**
 * Allows a build-time extension to extend the original deployment.
 *
 * @author Martin Kouba
 */
public interface DeploymentEnhancer extends BuildProcessor {

    /**
     *
     * @param deploymentContext
     */
    void enhance(DeploymentContext deploymentContext);

    interface DeploymentContext {

        /**
         *
         * @param clazz
         * @throws IllegalArgumentException If the class cannot be added to the index
         */
        void addClass(Class<?> clazz);

        /**
         *
         * @param className The fully qualified class name
         * @throws IllegalArgumentException If the class cannot be added to the index
         */
        void addClass(String className);

    }

}
