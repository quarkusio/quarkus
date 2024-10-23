package io.quarkus.flyway.deployment;

import org.jboss.jandex.AnnotationInstance;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.flyway.runtime.FlywayContainerProducer;
import io.quarkus.flyway.runtime.FlywayDataSourceBuildTimeConfig;

public final class FlywayBuildItem extends MultiBuildItem {

    private final Class<? extends FlywayContainerProducer> producerClass;
    private final FlywayDataSourceBuildTimeConfig buildTimeConfig;
    private final String name;
    private final String dataSourceName;
    private final AnnotationInstance qualifier;
    private final int priority;
    private final String containerBeanName;
    private final String flywayBeanName;

    public FlywayBuildItem(Class<? extends FlywayContainerProducer> producerClass,
            FlywayDataSourceBuildTimeConfig buildTimeConfig, String name, String dataSourceName, AnnotationInstance qualifier,
            int priority, String containerBeanName, String flywayBeanName) {
        this.producerClass = producerClass;
        this.buildTimeConfig = buildTimeConfig;
        this.name = name;
        this.dataSourceName = dataSourceName;
        this.qualifier = qualifier;
        this.priority = priority;
        this.containerBeanName = containerBeanName;
        this.flywayBeanName = flywayBeanName;
    }

    public Class<? extends FlywayContainerProducer> getProducerClass() {
        return producerClass;
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return producerClass + name;
    }

    public String getDataSourceName() {
        return dataSourceName;
    }

    public AnnotationInstance getQualifier() {
        return qualifier;
    }

    public int getPriority() {
        return priority;
    }

    public String getContainerBeanName() {
        return containerBeanName;
    }

    public String getFlywayBeanName() {
        return flywayBeanName;
    }

    public FlywayDataSourceBuildTimeConfig getBuildTimeConfig() {
        return buildTimeConfig;
    }
}
