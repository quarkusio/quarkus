package org.jboss.shamrock.deployment.builditem;

import java.util.logging.Level;

import org.jboss.builder.item.MultiBuildItem;
import org.wildfly.common.Assert;

/**
 * Establish the default log level of a log category.
 */
public final class LogCategoryBuildItem extends MultiBuildItem {
    private final String category;
    private final Level level;

    /**
     * Construct a new instance.
     *
     * @param category the category (must not be {@code null} or empty)
     * @param level the level (must not be {@code null})
     */
    public LogCategoryBuildItem(final String category, final Level level) {
        Assert.checkNotNullParam("category", category);
        Assert.checkNotEmptyParam("category", category);
        Assert.checkNotNullParam("level", level);
        this.category = category;
        this.level = level;
    }

    /**
     * Get the category.
     *
     * @return the category (not {@code null})
     */
    public String getCategory() {
        return category;
    }

    /**
     * Get the level.
     *
     * @return the level (not {@code null})
     */
    public Level getLevel() {
        return level;
    }
}
