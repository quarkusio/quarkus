package io.quarkus.flyway.deployment;

import org.jboss.jandex.AnnotationInstance;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.flyway.runtime.FlywayContainerProducer;
import io.quarkus.flyway.runtime.FlywayDataSourceBuildTimeConfig;

/**
 * Represents a build item that configures and provides Flyway instances for managing database migrations.
 *
 * <p>
 * This build item is responsible for initializing and configuring Flyway instances. This extension creates one instance
 * per datasource. The instances are distinguished by the {@code FlywayDataSource} qualifier. Additional instances can also be
 * added by
 * other extensions if required.
 *
 * <p>
 * Flyway instances are created by the {@code FlywayContainerProducer} implementation. Instances are differentiated by prefix
 * and name.
 * The name should be unique in the realm of an extension. The prefix is used to distinguish between different extensions.
 *
 * <p>
 * The following parameters allow to distinguish between Flyway instances:
 * <ul>
 * <li><strong>Qualifier</strong> and <strong>priority</strong> - Define the CDI qualifier and priority for the Flyway
 * instance.</li>
 * <li><strong>containerBeanName</strong> and <strong>flywayBeanName</strong> - Set up the {@code Named} qualifier for the
 * instance (can be null).</li>
 * </ul>
 *
 * @see org.flywaydb.core.Flyway
 */
public final class FlywayBuildItem extends MultiBuildItem {

    private final Class<? extends FlywayContainerProducer> producerClass;
    private final FlywayDataSourceBuildTimeConfig buildTimeConfig;
    private final String prefix;
    private final String name;
    private final String dataSourceName;
    private final AnnotationInstance qualifier;
    private final int priority;
    private final String containerBeanName;
    private final String flywayBeanName;

    public FlywayBuildItem(Class<? extends FlywayContainerProducer> producerClass,
            FlywayDataSourceBuildTimeConfig buildTimeConfig, String prefix, String name, String dataSourceName,
            AnnotationInstance qualifier,
            int priority, String containerBeanName, String flywayBeanName) {
        this.producerClass = producerClass;
        this.buildTimeConfig = buildTimeConfig;
        this.prefix = prefix;
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
        return prefix + "." + name;
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
