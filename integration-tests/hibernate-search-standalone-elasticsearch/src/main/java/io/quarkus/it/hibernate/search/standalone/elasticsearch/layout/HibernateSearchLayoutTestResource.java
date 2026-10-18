package io.quarkus.it.hibernate.search.standalone.elasticsearch.layout;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.hibernate.search.backend.elasticsearch.index.ElasticsearchIndexManager;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;

@Path("/test/layout-strategy")
public class HibernateSearchLayoutTestResource {

    @Inject
    SearchMapping searchMapping;

    @GET
    @Path("/index-name")
    public String name() {
        var descriptor = searchMapping.indexedEntity(LayoutEntity.class)
                .indexManager()
                .unwrap(ElasticsearchIndexManager.class)
                .descriptor();

        return "%s - %s - %s".formatted(descriptor.hibernateSearchName(), descriptor.readName(), descriptor.writeName());
    }
}
