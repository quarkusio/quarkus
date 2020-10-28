package io.quarkus.bootstrap.app;

import java.util.Set;

public interface AugmentAction {
    AugmentResult createProductionApplication();

    StartupAction createInitialRuntimeApplication();

    StartupAction reloadExistingApplication(boolean hasStartedSuccessfully, Set<String> changedResources,
            ClassChangeInformation classChangeInformation);
}
