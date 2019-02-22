package io.quarkus.deployment.devmode;

/**
 * Service interface that is used to abstract away the details of how hot deployment is performed
 */
public interface HotReplacementSetup {

    void setupHotDeployment(HotReplacementContext context);

}
