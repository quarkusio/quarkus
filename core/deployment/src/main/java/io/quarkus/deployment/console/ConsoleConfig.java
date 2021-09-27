package io.quarkus.deployment.console;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot
public class ConsoleConfig {

    /**
     * If test results and status should be displayed in the console.
     * <p>
     * If this is false results can still be viewed in the dev console.
     */
    @ConfigItem(defaultValue = "true")
    public boolean enabled;

    /**
     * Disables the ability to enter input on the console.
     */
    @ConfigItem(defaultValue = "false")
    public boolean disableInput;

    /**
     * Disable the testing status/prompt message at the bottom of the console
     * and log these messages to STDOUT instead.
     * <p>
     * Use this option if your terminal does not support ANSI escape sequences.
     */
    @ConfigItem(defaultValue = "false")
    public boolean basic;

}
