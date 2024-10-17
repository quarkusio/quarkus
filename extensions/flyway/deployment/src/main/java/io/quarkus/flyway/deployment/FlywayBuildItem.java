package io.quarkus.flyway.deployment;

import org.jboss.jandex.AnnotationInstance;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.flyway.runtime.ContainerProducer;
import io.quarkus.flyway.runtime.FlywayDataSourceBuildTimeConfig;

public final class FlywayBuildItem extends MultiBuildItem {

    public FlywayBuildItem(String name, String dataSourceName, boolean multiTenancyEnabled,
            FlywayDataSourceBuildTimeConfig buildTimeConfig, AnnotationInstance qualifier, String flywayBeanName,
            String containerBeanName, int priority, Class<? extends ContainerProducer> containerProducer) {
        this.name = name;
        this.dataSourceName = dataSourceName;
        this.multiTenancyEnabled = multiTenancyEnabled;
        this.buildTimeConfig = buildTimeConfig;
        this.qualifier = qualifier;
        this.flywayBeanName = flywayBeanName;
        this.containerBeanName = containerBeanName;
        this.priority = priority;
        this.containerProducer = containerProducer;
    }

    private final String name;
    private final String dataSourceName;
    private final boolean multiTenancyEnabled;
    private final FlywayDataSourceBuildTimeConfig buildTimeConfig;
    private final AnnotationInstance qualifier;
    private final String flywayBeanName;
    private final String containerBeanName;
    private final int priority;
    private final Class<? extends ContainerProducer> containerProducer;

    public String getName() {
        return name;
    }

    public String getDataSourceName() {
        return dataSourceName;
    }

    public boolean isMultiTenancyEnabled() {
        return multiTenancyEnabled;
    }

    public FlywayDataSourceBuildTimeConfig getBuildTimeConfig() {
        return buildTimeConfig;
    }

    public AnnotationInstance getQualifier() {
        return qualifier;
    }

    public int getPriority() {
        return priority;
    }

    public String getFlywayBeanName() {
        return flywayBeanName;
    }

    public String getContainerBeanName() {
        return containerBeanName;
    }

    public Class<? extends ContainerProducer> getContainerProducer() {
        return containerProducer;
    }
}
