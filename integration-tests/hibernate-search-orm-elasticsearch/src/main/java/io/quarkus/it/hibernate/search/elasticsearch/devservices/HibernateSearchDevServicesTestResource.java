package io.quarkus.it.hibernate.search.elasticsearch.devservices;

import java.util.List;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
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
