package io.quarkus.flyway.runtime;

import java.lang.annotation.Annotation;
import java.util.Map.Entry;

import javax.enterprise.inject.Default;
import javax.enterprise.util.AnnotationLiteral;

import org.flywaydb.core.Flyway;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.arc.runtime.BeanContainerListener;
import io.quarkus.flyway.FlywayDataSource;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class FlywayRecorder {

    public BeanContainerListener setFlywayBuildConfig(FlywayBuildTimeConfig flywayBuildConfig) {
        return beanContainer -> {
            FlywayProducer producer = beanContainer.instance(FlywayProducer.class);
            producer.setFlywayBuildConfig(flywayBuildConfig);
        };
    }

    public void configureFlywayProperties(FlywayRuntimeConfig flywayRuntimeConfig, BeanContainer container) {
        container.instance(FlywayProducer.class).setFlywayRuntimeConfig(flywayRuntimeConfig);
    }

    public void doStartActions(FlywayRuntimeConfig config, BeanContainer container) {
        if (config.defaultDataSource.cleanAtStart) {
            clean(container, Default.Literal.INSTANCE);
        }
        if (config.defaultDataSource.migrateAtStart) {
            migrate(container, Default.Literal.INSTANCE);
        }
        for (Entry<String, FlywayDataSourceRuntimeConfig> configPerDataSource : config.namedDataSources.entrySet()) {
            if (configPerDataSource.getValue().cleanAtStart) {
                clean(container, FlywayDataSource.FlywayDataSourceLiteral.of(configPerDataSource.getKey()));
            }
            if (configPerDataSource.getValue().migrateAtStart) {
                migrate(container, FlywayDataSource.FlywayDataSourceLiteral.of(configPerDataSource.getKey()));
            }
        }
    }

    private void clean(BeanContainer container, AnnotationLiteral<? extends Annotation> qualifier) {
        container.instance(Flyway.class, qualifier).clean();
    }

    private void migrate(BeanContainer container, AnnotationLiteral<? extends Annotation> qualifier) {
        container.instance(Flyway.class, qualifier).migrate();
    }
}
