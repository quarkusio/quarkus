package io.quarkus.hibernate.search.orm.elasticsearch.runtime.devconsole;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.hibernate.search.mapper.orm.entity.SearchIndexedEntity;
import org.hibernate.search.mapper.orm.mapping.SearchMapping;

import io.quarkus.devconsole.runtime.spi.DevConsolePostHandler;
import io.quarkus.devconsole.runtime.spi.FlashScopeUtil;
import io.quarkus.hibernate.search.orm.elasticsearch.runtime.HibernateSearchElasticsearchRuntimeConfig;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class HibernateSearchDevConsoleRecorder {

    public Supplier<HibernateSearchSupplier.IndexedPersistenceUnits> infoSupplier(
            HibernateSearchElasticsearchRuntimeConfig runtimeConfig, Set<String> persistenceUnitNames) {
        return new HibernateSearchSupplier(runtimeConfig, persistenceUnitNames);
    }

    public Handler<RoutingContext> indexEntity() {
        return new DevConsolePostHandler() {
            @Override
            protected void handlePostAsync(RoutingContext event, MultiMap form) throws Exception {
                if (form.isEmpty()) {
                    return;
                }
                Set<String> persitenceUnitNames = form.entries().stream().map(Map.Entry::getValue)
                        .collect(Collectors.toSet());
                Map<String, SearchMapping> mappings = HibernateSearchSupplier.searchMapping(persitenceUnitNames);
                if (mappings.isEmpty()) {
                    flashMessage(event, "There are no indexed entity types.", FlashScopeUtil.FlashMessageStatus.ERROR);
                    return;
                }
                for (Map.Entry<String, SearchMapping> entry : mappings.entrySet()) {
                    SearchMapping mapping = entry.getValue();
                    List<String> entityNames = mapping.allIndexedEntities().stream()
                            .map(SearchIndexedEntity::jpaName)
                            .filter(jpaName -> form.contains(jpaName, entry.getKey(), false))
                            .collect(Collectors.toList());
                    if (!entityNames.isEmpty()) {
                        mapping.scope(Object.class, entityNames).massIndexer()
                                .startAndWait();
                        flashMessage(event, "Entities successfully reindexed", Duration.ofSeconds(10));
                    }
                }
            }
        };
    }
}
