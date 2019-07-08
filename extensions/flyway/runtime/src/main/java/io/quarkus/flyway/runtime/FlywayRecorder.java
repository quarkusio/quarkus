package io.quarkus.flyway.runtime;

import org.flywaydb.core.Flyway;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.arc.runtime.BeanContainerListener;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class FlywayRecorder {

    public BeanContainerListener setFlywayBuildConfig(FlywayBuildConfig flywayBuildConfig) {
        return beanContainer -> {
            FlywayProducer producer = beanContainer.instance(FlywayProducer.class);
            producer.setFlywayBuildConfig(flywayBuildConfig);
        };
    }

    public void configureFlywayProperties(FlywayRuntimeConfig flywayRuntimeConfig, BeanContainer container) {
        container.instance(FlywayProducer.class).setFlywayRuntimeConfig(flywayRuntimeConfig);
    }

    public void doStartActions(FlywayRuntimeConfig config, BeanContainer container) {
        if (config.migrateAtStart) {
            Flyway flyway = container.instance(Flyway.class);
            flyway.migrate();
        }
    }
}
