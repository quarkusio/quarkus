package io.quarkus.hibernate.search.orm.elasticsearch.runtime.dev;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import org.hibernate.search.mapper.orm.mapping.SearchMapping;

import io.quarkus.arc.profile.IfBuildProfile;
import io.smallrye.mutiny.Multi;

@ApplicationScoped
@IfBuildProfile("dev")
public class HibernateSearchElasticsearchDevJsonRpcService {

    public HibernateSearchElasticsearchDevInfo getInfo() {
        return HibernateSearchElasticsearchDevController.get().getInfo();
    }

    public int getNumberOfPersistenceUnits() {
        return getInfo().getPersistenceUnits().size();
    }

    public int getNumberOfIndexedEntityTypes() {
        return getInfo().getNumberOfIndexedEntities();
    }

    public Multi<String> reindex(String puName, List<String> entityTypeNames) {
        SearchMapping mapping = HibernateSearchElasticsearchDevController.get().searchMapping(puName);

        return Multi.createBy().concatenating().streams(
                Multi.createFrom().item("started"),
                Multi.createFrom()
                        .completionStage(() -> mapping.scope(Object.class, entityTypeNames).massIndexer().start()
                                .thenApply(ignored -> "success"))
                        .onFailure().recoverWithItem(Throwable::getMessage));
    }

}
