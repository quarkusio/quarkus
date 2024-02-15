package io.quarkus.annotation.processor.documentation.config.model;

import java.util.Comparator;

public enum ConfigPhase implements Comparable<ConfigPhase> {

    RUN_TIME("RunTime"),
    BUILD_TIME("BuildTime"),
    BUILD_AND_RUN_TIME_FIXED("BuildTime");

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

    private String configSuffix;

    ConfigPhase(String configSuffix) {
        this.configSuffix = configSuffix;
    }

    public String getConfigSuffix() {
        return configSuffix;
    }
}
