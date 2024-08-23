package io.quarkus.it.hibernate.search.standalone.elasticsearch.management;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;

@Path("/test/management")
public class HibernateSearchManagementTestResource {

    @Inject
    SearchMapping searchMapping;
    @Inject
    MyDatastore datastore;

    @PUT
    @Path("/init-data")
    @Transactional
    public void initData() {
        datastore.clear();
        datastore.put(new ManagementTestEntity("name1"));
        datastore.put(new ManagementTestEntity("name2"));
        datastore.put(new ManagementTestEntity("name3"));
        datastore.put(new ManagementTestEntity("name4"));
        datastore.put(new ManagementTestEntity("name5"));
    }

    @GET
    @Path("/search-count")
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public long searchCount() {
        try (var searchSession = searchMapping.createSession()) {
            return searchSession.search(ManagementTestEntity.class)
                    .where(f -> f.matchAll())
                    .fetchTotalHitCount();
        }
    }
}
