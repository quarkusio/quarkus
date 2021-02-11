package io.quarkus.dev.spi;

/**
 * Service interface that is used to abstract away the details of how hot deployment is performed
 */
public interface HotReplacementSetup {

    void setupHotDeployment(HotReplacementContext context);

    /**
     * This method is called if the app fails to start the first time. This allows for hot deployment
     * providers to still start, and provide a way for the user to recover their app
     */
    default void handleFailedInitialStart() {
    };

    default void close() {
    };
}
