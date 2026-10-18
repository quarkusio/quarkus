package io.quarkus.deployment.ide;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * IDE
 */
@ConfigMapping(prefix = "quarkus.ide")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface IdeConfig {

    /**
     * The Ide to use to open files from the DevUI.
     * {@code auto} means that Quarkus will attempt to determine the Ide being used.
     */
    @WithDefault("auto")
    Target target();

    enum Target {
        auto,
        idea,
        vscode,
        eclipse,
        netbeans
    }
}
