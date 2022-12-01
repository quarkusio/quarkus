package io.quarkus.hibernate.reactive.runtime;

import java.util.List;
import java.util.Map;

import io.quarkus.hibernate.orm.runtime.HibernateOrmRuntimeConfig;
import io.quarkus.hibernate.orm.runtime.integration.HibernateOrmIntegrationRuntimeDescriptor;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class HibernateReactiveRecorder {

    /**
     * The feature needs to be initialized, even if it's not enabled.
     *
     * @param enabled Set to false if it's not being enabled, to log appropriately.
     */
    public void callHibernateReactiveFeatureInit(boolean enabled) {
        HibernateReactive.featureInit(enabled);
    }

    public void initializePersistenceProvider(HibernateOrmRuntimeConfig hibernateOrmRuntimeConfig,
            Map<String, List<HibernateOrmIntegrationRuntimeDescriptor>> integrationRuntimeDescriptors) {
        ReactivePersistenceProviderSetup.registerRuntimePersistenceProvider(hibernateOrmRuntimeConfig,
                integrationRuntimeDescriptors);
    }

}
