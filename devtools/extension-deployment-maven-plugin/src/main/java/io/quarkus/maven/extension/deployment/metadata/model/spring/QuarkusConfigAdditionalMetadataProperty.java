package io.quarkus.maven.extension.deployment.metadata.model.spring;

public record QuarkusConfigAdditionalMetadataProperty(ConfigPhase phase, String environmentVariable, boolean optional) {

    public enum ConfigPhase {
        RUN_TIME,
        BUILD_TIME,
        BUILD_AND_RUN_TIME_FIXED;

        public static ConfigPhase of(io.quarkus.annotation.processor.documentation.config.model.ConfigPhase phase) {
            switch (phase) {
                case BUILD_AND_RUN_TIME_FIXED:
                    return ConfigPhase.BUILD_AND_RUN_TIME_FIXED;
                case BUILD_TIME:
                    return ConfigPhase.BUILD_TIME;
                case RUN_TIME:
                    return ConfigPhase.RUN_TIME;
                default:
                    throw new IllegalStateException(
                            "Phase " + phase + " not supported in " + ConfigPhase.class.getSimpleName());
            }
        }
    }
}
