package io.quarkus.runtime.logging;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class CategoryBuildTimeConfig {
    /**
     * Specifies whether <code>isTraceEnabled</code> for a given category will return true or false for a given category.
     * This check is computed at build time to provide better hints on code paths that won't be used at runtime.
     *
     * It's common to see <code>isTraceEnabled</code> calls in sections that include building complex or expensive messages.
     * By using <code>isTraceEnabled</code> checks, users can double check that trace enabled before computing such a message.
     * Otherwise, complex messages could be constructed but then not used by the logging library if the runtime log level is not trace.
     * This build time option controls whether `isTraceEnabled` returns true or false at build time and makes that a constant at runtime,
     * irrespective of the runtime log level.
     *
     * Calls to <code>trace()</code> not preceded by <code>isTraceEnabled</code> are still governed by the runtime log level.
     * The message will only be printed if runtime log level for a given category is <code>TRACE</code>.
     */
    @ConfigItem(defaultValue = "false")
    public boolean buildTimeTraceEnabled;
}
