package io.quarkus.hibernate.search.standalone.elasticsearch.runtime.dev;

import java.util.List;

import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;

import io.smallrye.mutiny.Multi;

public class HibernateSearchStandaloneDevJsonRpcService {

    public HibernateSearchStandaloneDevInfo getInfo() {
        return HibernateSearchStandaloneDevController.get().getInfo();
    }

    public int getNumberOfIndexedEntityTypes() {
        return getInfo().getNumberOfIndexedEntities();
    }

    public Multi<String> reindex(List<String> entityTypeNames) {
        SearchMapping mapping = HibernateSearchStandaloneDevController.get().searchMapping();

        return Multi.createBy().concatenating().streams(
                Multi.createFrom().item("started"),
                Multi.createFrom()
                        .completionStage(() -> mapping.scope(Object.class, entityTypeNames).massIndexer().start()
                                .thenApply(ignored -> "success"))
                        .onFailure().recoverWithItem(Throwable::getMessage));
    }

}
