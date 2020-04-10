package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * This is an optional build item that allows us to track additional test classes that will become beans.
 * It is only available during tests
 */
public final class TestClassBeanBuildItem extends MultiBuildItem {

    private final String testClassName;

    public TestClassBeanBuildItem(String testClassName) {
        this.testClassName = testClassName;
    }

    public String getTestClassName() {
        return testClassName;
    }
}
