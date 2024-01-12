package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * This is an optional build item that represents the current test profile.
 * <p>
 * It is only available during tests.
 */
public final class TestProfileBuildItem extends SimpleBuildItem {

    private final String testProfileClassName;

    public TestProfileBuildItem(String testProfileClassName) {
        this.testProfileClassName = testProfileClassName;
    }

    public String getTestProfileClassName() {
        return testProfileClassName;
    }
}
