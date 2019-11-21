package io.quarkus.qute.runtime;

import java.util.List;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class QuteConfig {

    /**
     * A path relative from {@code META-INF/resources/}. All files in the base directory and its subdirectories are considered
     * templates and watched for changes in the development mode.
     */
    @ConfigItem(defaultValue = "templates")
    public String basePath;

    /**
     * The set of suffixes used when attempting to locate a template.
     */
    @ConfigItem(defaultValue = "html,txt")
    public List<String> suffixes;

}
