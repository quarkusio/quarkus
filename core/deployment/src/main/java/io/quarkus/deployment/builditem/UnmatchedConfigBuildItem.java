package io.quarkus.deployment.builditem;

import java.util.Set;

import org.wildfly.common.Assert;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * An internal build item which relays the unmatched configuration key set from the extension loader
 * to configuration setup stages.
 */
public final class UnmatchedConfigBuildItem extends SimpleBuildItem {
    private final Set<String> set;

    /**
     * Construct a new instance.
     *
     * @param set the non-{@code null}, immutable set
     */
    public UnmatchedConfigBuildItem(final Set<String> set) {
        Assert.checkNotNullParam("set", set);
        this.set = set;
    }

    /**
     * Get the set.
     *
     * @return the set
     */
    public Set<String> getSet() {
        return set;
    }
}
