package io.quarkus.hibernate.search.standalone.elasticsearch.runtime.dev;

import io.quarkus.hibernate.search.standalone.elasticsearch.runtime.HibernateSearchStandaloneRuntimeConfig;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class HibernateSearchStandaloneDevRecorder {

    public void initController(boolean enabled, HibernateSearchStandaloneRuntimeConfig runtimeConfig) {
        HibernateSearchStandaloneDevController.get().setActive(enabled && runtimeConfig.active().orElse(true));
    }
}
