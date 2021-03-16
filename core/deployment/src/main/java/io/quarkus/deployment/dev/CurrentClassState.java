package io.quarkus.deployment.dev;

import java.util.List;
import java.util.Map;

import io.quarkus.bootstrap.model.AppArtifactKey;

public class CurrentClassState {

    private final List<CurrentModuleState> currentModuleState;

    public CurrentClassState(List<CurrentModuleState> currentModuleState) {
        this.currentModuleState = currentModuleState;
    }

    public List<CurrentModuleState> getCurrentModuleState() {
        return currentModuleState;
    }

    public static class CurrentModuleState {

        private final AppArtifactKey module;
        private final Map<String, String> fileToHash;

        public CurrentModuleState(AppArtifactKey module, Map<String, String> fileToHash) {
            this.module = module;
            this.fileToHash = fileToHash;
        }

        public AppArtifactKey getModule() {
            return module;
        }

        public Map<String, String> getFileToHash() {
            return fileToHash;
        }
    }

}
