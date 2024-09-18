package io.quarkus.it.hibernate.search.orm.elasticsearch.layout;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.hibernate.SessionFactory;
import org.hibernate.search.backend.elasticsearch.index.ElasticsearchIndexManager;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.mapper.orm.mapping.SearchMapping;

@Path("/test/layout-strategy")
public class HibernateSearchLayoutTestResource {

    @Inject
    SessionFactory sessionFactory;

    @Inject
    SearchMapping searchMapping;

    @GET
    @Path("/property")
    @Transactional
    public String layoutStrategy() {
        return ((BeanReference<?>) sessionFactory.getProperties().get("hibernate.search.backend.layout.strategy")).toString();
    }

    @GET
    @Path("/index-name")
    @Transactional
    public String name() {
        var descriptor = searchMapping.indexedEntity(LayoutEntity.class)
                .indexManager()
                .unwrap(ElasticsearchIndexManager.class)
                .descriptor();

        return "%s - %s - %s".formatted(descriptor.hibernateSearchName(), descriptor.readName(), descriptor.writeName());
    }
}
