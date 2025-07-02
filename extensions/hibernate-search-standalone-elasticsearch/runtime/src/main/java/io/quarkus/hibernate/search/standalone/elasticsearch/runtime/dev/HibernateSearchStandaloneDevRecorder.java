package io.quarkus.hibernate.search.standalone.elasticsearch.runtime.dev;

import io.quarkus.hibernate.search.standalone.elasticsearch.runtime.HibernateSearchStandaloneRuntimeConfig;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class HibernateSearchStandaloneDevRecorder {
    private final RuntimeValue<HibernateSearchStandaloneRuntimeConfig> runtimeConfig;

    public HibernateSearchStandaloneDevRecorder(final RuntimeValue<HibernateSearchStandaloneRuntimeConfig> runtimeConfig) {
        this.runtimeConfig = runtimeConfig;
    }

    public void initController(boolean enabled) {
        HibernateSearchStandaloneDevController.get().setActive(enabled && runtimeConfig.getValue().active().orElse(true));
    }
}
