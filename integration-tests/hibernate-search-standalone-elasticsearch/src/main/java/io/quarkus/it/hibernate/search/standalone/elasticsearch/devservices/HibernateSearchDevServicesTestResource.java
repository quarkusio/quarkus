package io.quarkus.it.hibernate.search.standalone.elasticsearch.devservices;

import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.elasticsearch.client.RestClient;
import org.hibernate.search.backend.elasticsearch.ElasticsearchBackend;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;

@Path("/test/dev-services")
public class HibernateSearchDevServicesTestResource {

    @Inject
    SearchMapping searchMapping;

    @GET
    @Path("/hosts")
    @Transactional
    public String hosts() {
        return searchMapping.backend().unwrap(ElasticsearchBackend.class).client(RestClient.class)
                .getNodes().stream()
                .map(n -> n.getHost().toHostString())
                .collect(Collectors.joining());
    }

    @PUT
    @Path("/init-data")
    @Transactional
    public void initData() {
        try (var searchSession = searchMapping.createSession()) {
            IndexedEntity entity = new IndexedEntity(1, "John Irving");
            searchSession.indexingPlan().add(entity);
        }
        searchMapping.scope(IndexedEntity.class).workspace().refresh();
    }

    @GET
    @Path("/count")
    @Produces(MediaType.TEXT_PLAIN)
    public long count() {
        try (var searchSession = searchMapping.createSession()) {
            return searchSession.search(IndexedEntity.class)
                    .where(f -> f.matchAll())
                    .fetchTotalHitCount();
        }
    }
}
