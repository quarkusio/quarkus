package io.quarkus.bootstrap.app;

import java.util.Set;

public interface AugmentAction {
    AugmentResult createProductionApplication();

    StartupAction createInitialRuntimeApplication();

    StartupAction reloadExistingApplication(Set<String> changedResources);
}
