package io.quarkus.banner;

import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "banner")
public class BannerConfig {

    private static final String DEFAULT_BANNER_FILE = "default_banner.txt";

    /**
     * The path of the banner (path relative to root of classpath)
     * which could be provided by user
     */
    @ConfigItem(defaultValue = DEFAULT_BANNER_FILE)
    public String path;

    /**
     * Configuration for the graphical banner.
     */
    @ConfigDocSection
    public ImageConfig image;

    public boolean isDefaultPath() {
        return DEFAULT_BANNER_FILE.equals(path);
    }

    @ConfigGroup
    public static class ImageConfig {
        private static final String DEFAULT_GRAPHICAL_BANNER_FILE = "default_banner.png";

        /**
         * The path of the graphical banner (relative to the class path root).
         */
        @ConfigItem(defaultValue = DEFAULT_GRAPHICAL_BANNER_FILE)
        public String path;

        /**
         * The aspect ratio of the font for the graphical banner.
         */
        @ConfigItem(defaultValue = "0.6")
        public float fontAspectRatio;

        /**
         * The number of text rows for the graphical banner.
         * If omitted, the value will be calculated based on the {@code columns} property;
         * if that value is also omitted then the original image size will be used.
         */
        @ConfigItem(defaultValue = "5")
        public OptionalInt rows;

        /**
         * The number of text rows for the graphical banner.
         * If omitted, the value will be calculated based on the {@code rows} property;
         * if that value is also omitted then the original image size will be used.
         */
        @ConfigItem
        public OptionalInt columns;

        /**
         * Force the image scale.
         * Use if your terminal does not correctly scale the image, causing it to appear squashed or stretched.
         */
        @ConfigItem(defaultValue = "false")
        public boolean forceScale;
    }
}
