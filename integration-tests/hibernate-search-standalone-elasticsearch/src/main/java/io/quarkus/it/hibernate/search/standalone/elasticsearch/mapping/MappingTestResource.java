package io.quarkus.it.hibernate.search.standalone.elasticsearch.mapping;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;

@Path("/test/mapping")
public class MappingTestResource {

    @Inject
    SearchMapping searchMapping;

    @PUT
    @Path("/init-data")
    @Transactional
    public void initData() {
        try (var session = searchMapping.createSession()) {
            session.indexingPlan().add(new EntityMappedProgrammatically(42, "sometext"));
        }
    }

    @GET
    @Path("/programmatic")
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public String testProgrammaticMapping() {
        try (var session = searchMapping.createSession()) {
            assertThat(session.search(EntityMappedProgrammatically.class)
                    .select(f -> f.field("text"))
                    .where(f -> f.match().field("text").matching("sometext"))
                    .fetchAllHits())
                    .hasSize(1)
                    .containsOnly("sometext");
        }
        return "OK";
    }
}
