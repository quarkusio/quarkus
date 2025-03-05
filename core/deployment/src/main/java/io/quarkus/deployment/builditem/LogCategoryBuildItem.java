package io.quarkus.deployment.builditem;

import java.util.logging.Level;

import io.quarkus.builder.item.MultiBuildItem;
import io.smallrye.common.constraint.Assert;

/**
 * Establish the default log level of a log category.
 */
public final class LogCategoryBuildItem extends MultiBuildItem {
    private final String category;
    private final Level level;
    private final boolean setMinLevelDefault;

    /**
     * Construct a new instance.
     *
     * @param category the category (must not be {@code null} or empty)
     * @param level the level (must not be {@code null})
     */
    public LogCategoryBuildItem(final String category, final Level level) {
        this(category, level, false);
    }

    /**
     * Construct a new instance.
     *
     * @param category the category (must not be {@code null} or empty)
     * @param level the level (must not be {@code null})
     * @param setMinLevelDefault if the default minimum level for the category should be set
     */
    public LogCategoryBuildItem(final String category, final Level level, boolean setMinLevelDefault) {
        Assert.checkNotNullParam("category", category);
        Assert.checkNotEmptyParam("category", category);
        Assert.checkNotNullParam("level", level);
        this.category = category;
        this.level = level;
        this.setMinLevelDefault = setMinLevelDefault;
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

    /**
     * @return {@code true} if the default min-level for the category should also be set.
     */
    public boolean isSetMinLevelDefault() {
        return setMinLevelDefault;
    }
}
