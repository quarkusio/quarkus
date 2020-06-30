package io.quarkus.hibernate.orm.runtime.boot;

import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.internal.BootstrapServiceRegistryImpl;
import org.hibernate.boot.registry.selector.spi.StrategySelector;

import io.quarkus.hibernate.orm.runtime.customized.QuarkusIntegratorServiceImpl;
import io.quarkus.hibernate.orm.runtime.customized.QuarkusStrategySelectorBuilder;
import io.quarkus.hibernate.orm.runtime.recording.RecordableBootstrap;
import io.quarkus.hibernate.orm.runtime.service.FlatClassLoaderService;

final class RecordableBootstrapFactory {

    public static RecordableBootstrap createRecordableBootstrapBuilder(QuarkusPersistenceUnitDefinition puDefinition) {
        final BootstrapServiceRegistry bsr = buildBootstrapServiceRegistry();
        final RecordableBootstrap ssrBuilder = new RecordableBootstrap(bsr);
        return ssrBuilder;
    }

    private static BootstrapServiceRegistry buildBootstrapServiceRegistry() {
        final ClassLoaderService providedClassLoaderService = FlatClassLoaderService.INSTANCE;
        // N.B. support for custom IntegratorProvider injected via Properties (as
        // instance) removed

        final QuarkusIntegratorServiceImpl integratorService = new QuarkusIntegratorServiceImpl(providedClassLoaderService);
        final QuarkusStrategySelectorBuilder strategySelectorBuilder = new QuarkusStrategySelectorBuilder();
        final StrategySelector strategySelector = strategySelectorBuilder.buildSelector(providedClassLoaderService);
        return new BootstrapServiceRegistryImpl(true, providedClassLoaderService, strategySelector, integratorService);
    }

}
