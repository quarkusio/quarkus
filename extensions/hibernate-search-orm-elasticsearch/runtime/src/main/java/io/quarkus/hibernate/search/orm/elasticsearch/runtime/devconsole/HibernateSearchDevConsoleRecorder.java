package io.quarkus.hibernate.search.orm.elasticsearch.runtime.devconsole;

import java.time.Duration;
import java.util.stream.Collectors;

import org.hibernate.search.mapper.orm.entity.SearchIndexedEntity;
import org.hibernate.search.mapper.orm.mapping.SearchMapping;

import io.quarkus.devconsole.runtime.spi.DevConsolePostHandler;
import io.quarkus.devconsole.runtime.spi.FlashScopeUtil;
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
                SearchMapping mapping = HibernateSearchSupplier.searchMapping();
                if (mapping == null) {
                    flashMessage(event, "There are no indexed entity types.", FlashScopeUtil.FlashMessageStatus.ERROR);
                    return;
                }
                mapping.scope(Object.class,
                        mapping.allIndexedEntities().stream()
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
