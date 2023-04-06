package io.quarkus.kubernetes.deployment;

import java.util.Optional;

import io.fabric8.kubernetes.client.Config;

public interface DeploymentDecorator<D extends DeploymentDecorator<D>> {

    /**
     * Check if the decoratora has been applied.
     *
     * @return true if it has.
     */
    public boolean isApplied();

    /**
     * An optional message to display when applying the Decrator.
     * As this decorator is not affecting files, its usually a good idea
     * to log a message so that the user know what changed during deployment.
     * This method is meant to be used for that purpose.
     *
     * @return an {@link Optional} message.
     */
    default Optional<String> getMessage() {
        return Optional.empty();
    }

    /**
     * Return an instance of the decroator that is aware of the kubernetes config.
     *
     * @param config The kubernetes config.
     * @return an instance of the decorator.
     */
    public D withDeploymentContext(Config config);
}
