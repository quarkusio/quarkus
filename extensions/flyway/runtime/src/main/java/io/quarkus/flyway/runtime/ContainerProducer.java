package io.quarkus.flyway.runtime;

import java.lang.annotation.Annotation;
import java.util.List;

import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.flyway.FlywayConfigurationCustomizer;

public interface ContainerProducer {

    String getTenantId(SyntheticCreationalContext<?> context);

    Annotation getFlywayContainerQualifier(String name);

    FlywayDataSourceBuildTimeConfig getBuildTimeConfig(String name);

    FlywayDataSourceRuntimeConfig getRuntimeConfig(String name);

    List<FlywayConfigurationCustomizer> matchingConfigCustomizers(String dataSourceName);
}
