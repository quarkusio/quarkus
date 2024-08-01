package io.quarkus.annotation.processor.documentation.config.model;

import java.util.Comparator;

public enum ConfigPhase implements Comparable<ConfigPhase> {

    RUN_TIME("RunTime", false),
    BUILD_TIME("BuildTime", true),
    BUILD_AND_RUN_TIME_FIXED("BuildTime", true);

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

    private final String configSuffix;
    private final boolean fixedAtBuildTime;

    ConfigPhase(String configSuffix, boolean fixedAtBuildTime) {
        this.configSuffix = configSuffix;
        this.fixedAtBuildTime = fixedAtBuildTime;
    }

    public String getConfigSuffix() {
        return configSuffix;
    }

    public boolean isFixedAtBuildTime() {
        return fixedAtBuildTime;
    }
}
