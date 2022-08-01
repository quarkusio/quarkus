package io.quarkus.bootstrap.app;

import java.util.Set;

public interface AugmentAction {

    /**
     * Performs a custom augmentation run, asking for the outputs provided in finalOutputs, and passing them to the
     * resultConsumer
     *
     * @param resultConsumer The name of a {@code BiConsumer<Object, BuildResult>} class that will be instantiated to handle the
     *        result
     * @param context An additional context object that is passed as-is to the result consumer
     * @param finalOutputs The names of the build items to ask for as part of the build
     */
    void performCustomBuild(String resultConsumer, Object context, String... finalOutputs);

    AugmentResult createProductionApplication();

    StartupAction createInitialRuntimeApplication();

    StartupAction reloadExistingApplication(boolean hasStartedSuccessfully, Set<String> changedResources,
            ClassChangeInformation classChangeInformation);
}
