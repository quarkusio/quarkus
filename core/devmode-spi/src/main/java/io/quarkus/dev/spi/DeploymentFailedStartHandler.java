package io.quarkus.dev.spi;

public interface DeploymentFailedStartHandler {

    /**
     * This method is called if the app fails to start the first time. This allows for hot deployment
     * providers to still start, and provide a way for the user to recover their app
     */
    void handleFailedInitialStart();

}
