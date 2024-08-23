package io.quarkus.hibernate.search.orm.elasticsearch.runtime.dev;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import io.quarkus.hibernate.search.orm.elasticsearch.runtime.HibernateSearchElasticsearchRuntimeConfig;
import io.quarkus.hibernate.search.orm.elasticsearch.runtime.HibernateSearchElasticsearchRuntimeConfigPersistenceUnit;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class HibernateSearchElasticsearchDevRecorder {

    public void initController(
            HibernateSearchElasticsearchRuntimeConfig runtimeConfig, Set<String> persistenceUnitNames) {
        Map<String, HibernateSearchElasticsearchRuntimeConfigPersistenceUnit> puConfigs = runtimeConfig
                .persistenceUnits();
        Set<String> activePersistenceUnitNames = persistenceUnitNames.stream()
                .filter(name -> {
                    var puConfig = puConfigs.get(name);
                    return puConfig == null || puConfig.active().orElse(true);
                })
                .collect(Collectors.toCollection(LinkedHashSet::new));
        HibernateSearchElasticsearchDevController.get().setActivePersistenceUnitNames(activePersistenceUnitNames);
    }
}
