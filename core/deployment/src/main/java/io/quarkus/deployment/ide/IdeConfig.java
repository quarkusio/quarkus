package io.quarkus.deployment.ide;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * IDE
 */
@ConfigRoot
public class IdeConfig {

    /**
     * The Ide to use to open files from the DevUI.
     * {@code auto} means that Quarkus will attempt to determine the Ide being used.
     */
    @ConfigItem(defaultValue = "auto")
    public Target target;

    enum Target {
        auto,
        idea,
        vscode,
        eclipse,
        netbeans
    }
}
