package io.quarkus.it.hibernate.search.orm.elasticsearch.management;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.hibernate.Session;
import org.hibernate.search.mapper.orm.session.SearchSession;

@Path("/test/management")
public class HibernateSearchManagementTestResource {

    @Inject
    Session session;

    @Inject
    SearchSession searchSession;

    @PUT
    @Path("/init-data")
    @Transactional
    public void initData() {
        searchSession.indexingPlanFilter(context -> context.exclude(Object.class));
        session.persist(new ManagementTestEntity("name1"));
        session.persist(new ManagementTestEntity("name2"));
        session.persist(new ManagementTestEntity("name3"));
        session.persist(new ManagementTestEntity("name4"));
        session.persist(new ManagementTestEntity("name5"));
    }

    @GET
    @Path("/search-count")
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public long testAnalysisConfigured() {
        return searchSession.search(ManagementTestEntity.class)
                .select(f -> f.id())
                .where(f -> f.matchAll())
                .fetchTotalHitCount();
    }
}
