package io.quarkus.runtime;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public class BannerRuntimeConfig {

    /**
     * Whether the banner will be displayed
     */
    @ConfigItem(defaultValue = "true")
    public boolean enabled;

    /**
     * Set to send terminal codes around the banner to prevent wrapping in the event that it exceeds the window size.
     */
    @ConfigItem(defaultValue = "true")
    public boolean noWrap;

    /**
     * Run time configuration for the graphical banner.
     */
    public ImageConfig image;

    @ConfigGroup
    public static class ImageConfig {

        /**
         * Specifies whether the graphical banner will be displayed.
         */
        @ConfigItem(defaultValue = "false")
        public boolean enabled;
    }
}
