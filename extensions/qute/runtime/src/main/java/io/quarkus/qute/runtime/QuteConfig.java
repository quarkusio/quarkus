package io.quarkus.qute.runtime;

import java.util.List;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class QuteConfig {

    /**
     * The set of suffixes used when attempting to locate a template file.
     * 
     * By default, `engine.getTemplate("foo")` would result in several lookups: `src/main/resources/templates/foo`,
     * `src/main/resources/templates/foo.html` and `src/main/resources/templates/foo.txt`.
     * 
     * @asciidoclet
     */
    @ConfigItem(defaultValue = "html,txt")
    public List<String> suffixes;

}
