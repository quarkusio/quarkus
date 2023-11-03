package io.quarkus.agroal.runtime;

import java.util.List;

import jakarta.enterprise.event.Observes;

import io.agroal.api.AgroalDataSource;
import io.quarkus.arc.All;
import io.quarkus.runtime.StartupEvent;

/**
 * Make sure datasources are initialized at startup.
 */
public class AgroalDataSourcesInitializer {

    void init(@Observes StartupEvent event, @All List<AgroalDataSource> dataSources) {
        // nothing to do here, eager injection will initialize the beans
    }
}
