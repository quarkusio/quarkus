package io.quarkus.it.hibernate.search.orm.elasticsearch.devservices;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.search.mapper.orm.schema.management.SchemaManagementStrategyName;
import org.hibernate.search.mapper.orm.session.SearchSession;

@Path("/test/dev-services")
public class HibernateSearchDevServicesTestResource {

    @Inject
    SessionFactory sessionFactory;

    @Inject
    Session session;

    @Inject
    SearchSession searchSession;

    @GET
    @Path("/hosts")
    @Transactional
    @SuppressWarnings("unchecked")
    public String hosts() {
        return ((List<String>) sessionFactory.getProperties().get("hibernate.search.backend.hosts")).iterator().next();
    }

    @GET
    @Path("/schema-management-strategy")
    @Transactional
    public String schemaManagementStrategy() {
        var strategy = ((SchemaManagementStrategyName) sessionFactory.getProperties()
                .get("hibernate.search.schema_management.strategy"));
        return strategy == null ? null : strategy.externalRepresentation();
    }

    @PUT
    @Path("/init-data")
    @Transactional
    public void initData() {
        IndexedEntity entity = new IndexedEntity("John Irving");
        session.persist(entity);
    }

    @PUT
    @Path("/refresh")
    @Produces(MediaType.TEXT_PLAIN)
    public String refresh() {
        searchSession.workspace().refresh();
        return "OK";
    }

    @GET
    @Path("/count")
    @Produces(MediaType.TEXT_PLAIN)
    public long count() {
        return searchSession.search(IndexedEntity.class)
                .where(f -> f.matchAll())
                .fetchTotalHitCount();
    }
}
