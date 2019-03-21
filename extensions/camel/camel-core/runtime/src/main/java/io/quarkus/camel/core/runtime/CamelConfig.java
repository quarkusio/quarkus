package io.quarkus.camel.core.runtime;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

public class CamelConfig {

    @ConfigRoot(name = "camel", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
    public static class BuildTime {

        /**
         * Uri to an xml containing camel routes to be loaded and initialized at build time.
         */
        @ConfigItem(defaultValue = "file:./camel-routes.xml")
        public String routesUri;

        /**
         * Defer Camel context initialization phase until runtime.
         */
        @ConfigItem(defaultValue = "false")
        public boolean deferInitPhase;

    }

    @ConfigRoot(name = "camel", phase = ConfigPhase.RUN_TIME)
    public static class Runtime {

        /**
         * Dump loaded routes when starting
         */
        @ConfigItem(defaultValue = "false")
        public boolean dumpRoutes;
    }

}
