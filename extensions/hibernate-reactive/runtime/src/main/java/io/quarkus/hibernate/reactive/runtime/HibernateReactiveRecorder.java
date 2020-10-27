package io.quarkus.hibernate.reactive.runtime;

import io.quarkus.hibernate.orm.runtime.HibernateOrmRuntimeConfig;
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

    public void initializePersistenceProvider(HibernateOrmRuntimeConfig hibernateOrmRuntimeConfig) {
        ReactivePersistenceProviderSetup.registerRuntimePersistenceProvider(hibernateOrmRuntimeConfig);
    }

}
