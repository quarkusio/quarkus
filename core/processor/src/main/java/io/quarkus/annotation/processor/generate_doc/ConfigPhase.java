package io.quarkus.annotation.processor.generate_doc;

import io.quarkus.annotation.processor.Constants;

public enum ConfigPhase {
    RUN_TIME("The configuration is overridable at runtime", Constants.CONFIG_PHASE_RUNTIME_ILLUSTRATION),
    BUILD_TIME("The configuration is not overridable at runtime", Constants.CONFIG_PHASE_BUILD_TIME_ILLUSTRATION),
    BUILD_AND_RUN_TIME_FIXED("The configuration is not overridable at runtime", Constants.CONFIG_PHASE_BUILD_TIME_ILLUSTRATION);

    private String description;
    private String illustration;

    ConfigPhase(String description, String illustration) {
        this.description = description;
        this.illustration = illustration;
    }

    @Override
    public String toString() {
        return "ConfigPhase{" +
                "description='" + description + '\'' +
                ", illustration='" + illustration + '\'' +
                '}';
    }

    public String getIllustration() {
        return illustration;
    }
}
