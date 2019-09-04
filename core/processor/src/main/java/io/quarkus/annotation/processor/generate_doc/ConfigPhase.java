package io.quarkus.annotation.processor.generate_doc;

import java.util.Comparator;

import io.quarkus.annotation.processor.Constants;

public enum ConfigPhase implements Comparable<ConfigPhase> {
    RUN_TIME("The configuration is overridable at runtime", Constants.CONFIG_PHASE_RUNTIME_ILLUSTRATION),
    BUILD_TIME("The configuration is not overridable at runtime", Constants.CONFIG_PHASE_BUILD_TIME_ILLUSTRATION),
    BUILD_AND_RUN_TIME_FIXED("The configuration is not overridable at runtime", Constants.CONFIG_PHASE_BUILD_TIME_ILLUSTRATION);

    static final Comparator<ConfigPhase> COMPARATOR = new Comparator<ConfigPhase>() {
        /**
         * Order built time phase first
         * Then build time run time fixed phase
         * Then runtime one
         */
        @Override
        public int compare(ConfigPhase firstPhase, ConfigPhase secondPhase) {
            switch (firstPhase) {
                case BUILD_TIME: {
                    switch (secondPhase) {
                        case BUILD_TIME:
                            return 0;
                        default:
                            return -1;
                    }
                }
                case BUILD_AND_RUN_TIME_FIXED: {
                    switch (secondPhase) {
                        case BUILD_TIME:
                            return 1;
                        case BUILD_AND_RUN_TIME_FIXED:
                            return 0;
                        default:
                            return -1;
                    }
                }
                case RUN_TIME: {
                    switch (secondPhase) {
                        case RUN_TIME:
                            return 0;
                        default:
                            return 1;
                    }
                }
                default:
                    return 0;
            }
        }
    };

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
