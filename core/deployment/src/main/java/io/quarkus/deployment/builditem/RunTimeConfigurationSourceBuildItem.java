package io.quarkus.deployment.builditem;

import java.util.OptionalInt;

import org.jboss.builder.item.MultiBuildItem;
import org.wildfly.common.Assert;

/**
 * Define an additional configuration source which is used at run time.
 */
public final class RunTimeConfigurationSourceBuildItem extends MultiBuildItem {
    private final String className;
    private final OptionalInt priority;

    /**
     * Construct a new instance.
     *
     * @param className the class name (must not be {@code null} or empty)
     * @param priority an optional priority value to pass to the constructor (must not be {@code null})
     */
    public RunTimeConfigurationSourceBuildItem(final String className, final OptionalInt priority) {
        Assert.checkNotNullParam("className", className);
        Assert.checkNotEmptyParam("className", className);
        Assert.checkNotNullParam("priority", priority);
        this.className = className;
        this.priority = priority;
    }

    /**
     * Get the class name.
     *
     * @return the class name (not {@code null} or empty)
     */
    public String getClassName() {
        return className;
    }

    /**
     * Get the priority. If present, the priority value will be passed into the constructor of the configuration source
     * class; if absent, a no-arg constructor will be used.
     *
     * @return the priority (not {@code null})
     */
    public OptionalInt getPriority() {
        return priority;
    }
}
