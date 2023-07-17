package io.quarkus.extest.deployment;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.extest.runtime.IConfigConsumer;

/**
 * Represent beans annotated with @TestAnnotation that also implement IConfigConsumer
 */
public final class TestBeanBuildItem extends MultiBuildItem {
    private Class<IConfigConsumer> configConsumer;

    public TestBeanBuildItem(Class<IConfigConsumer> configConsumer) {
        this.configConsumer = configConsumer;
    }

    public Class<IConfigConsumer> getConfigConsumer() {
        return configConsumer;
    }
}
