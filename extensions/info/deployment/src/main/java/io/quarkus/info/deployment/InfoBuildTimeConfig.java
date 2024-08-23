package io.quarkus.info.deployment;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithParentName;

@ConfigMapping(prefix = "quarkus.info")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface InfoBuildTimeConfig {

    /**
     * Whether the info endpoint will be enabled
     */
    @WithDefault("true")
    boolean enabled();

    /**
     * The path under which the info endpoint will be located
     */
    @WithDefault("info")
    String path();

    /**
     * Git related configuration
     */
    Git git();

    /**
     * Build related configuration
     */
    Build build();

    /**
     * Build related configuration
     */
    Os os();

    /**
     * Build related configuration
     */
    Java java();

    interface Git {

        /**
         * Whether git info will be included in the info endpoint
         */
        @WithDefault("true")
        boolean enabled();

        /**
         * Controls how much information is present in the git section
         */
        @WithDefault("standard")
        Mode mode();

        enum Mode {
            STANDARD,
            FULL
        }
    }

    interface Build {

        /**
         * Whether build info will be included in the info endpoint
         */
        @WithDefault("true")
        boolean enabled();

        /**
         * Additional properties to be added to the build section
         */
        @WithParentName
        @ConfigDocMapKey("property-key")
        Map<String, String> additionalProperties();
    }

    interface Os {

        /**
         * Whether os info will be included in the info endpoint
         */
        @WithDefault("true")
        boolean enabled();
    }

    interface Java {

        /**
         * Whether java info will be included in the info endpoint
         */
        @WithDefault("true")
        boolean enabled();
    }
}
