package io.quarkus.hibernate.search.orm.elasticsearch.runtime.devconsole;

import java.time.Duration;
import java.util.stream.Collectors;

import org.hibernate.search.mapper.orm.entity.SearchIndexedEntity;
import org.hibernate.search.mapper.orm.mapping.SearchMapping;

import io.quarkus.devconsole.runtime.spi.DevConsolePostHandler;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class HibernateSearchDevConsoleRecorder {

    public Handler<RoutingContext> indexEntity() {
        return new DevConsolePostHandler() {
            @Override
            protected void handlePostAsync(RoutingContext event, MultiMap form) throws Exception {
                if (form.isEmpty()) {
                    return;
                }
                SearchMapping searchMapping = HibernateSearchSupplier.searchMapping();
                searchMapping.scope(Object.class,
                        searchMapping.allIndexedEntities().stream()
                                .map(SearchIndexedEntity::jpaName)
                                .filter(form::contains)
                                .collect(Collectors.toList()))
                        .massIndexer()
                        .startAndWait();
                flashMessage(event, "Entities successfully reindexed", Duration.ofSeconds(10));
            }
        };
    }
}
