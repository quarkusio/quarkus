package io.quarkus.deployment.dev;

import java.util.List;
import java.util.Map;

import io.quarkus.maven.dependency.ArtifactKey;

public class CurrentClassState {

    private final List<CurrentModuleState> currentModuleState;

    public CurrentClassState(List<CurrentModuleState> currentModuleState) {
        this.currentModuleState = currentModuleState;
    }

    public List<CurrentModuleState> getCurrentModuleState() {
        return currentModuleState;
    }

    public static class CurrentModuleState {

        private final ArtifactKey module;
        private final Map<String, String> fileToHash;

        public CurrentModuleState(ArtifactKey module, Map<String, String> fileToHash) {
            this.module = module;
            this.fileToHash = fileToHash;
        }

        public ArtifactKey getModule() {
            return module;
        }

        public Map<String, String> getFileToHash() {
            return fileToHash;
        }
    }

}
