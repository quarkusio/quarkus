package io.quarkus.it.hibernate.search.standalone.elasticsearch.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.hibernate.search.engine.common.EntityReference;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.jboss.resteasy.annotations.jaxrs.QueryParam;

@Path("/test/analysis")
public class AnalysisTestResource {

    @Inject
    SearchMapping searchMapping;

    @PUT
    @Path("/init-data")
    public void initData() {
        try (var session = searchMapping.createSession()) {
            for (var entity : List.of(new Analysis0TestingEntity(0, "irrelevant"),
                    new Analysis1TestingEntity(1, "irrelevant"),
                    new Analysis2TestingEntity(2, "irrelevant"))) {
                session.indexingPlan().add(entity);
            }
        }
    }

    @GET
    @Path("/analysis-configured")
    @Produces(MediaType.TEXT_PLAIN)
    public String testAnalysisConfigured() {
        assertThat(findTypesMatching("text", "token_inserted_by_backend_analysis"))
                .containsExactlyInAnyOrder(Analysis0TestingEntity.class);

        assertThat(findTypesMatching("text", "token_inserted_by_index_analysis_1"))
                .containsExactlyInAnyOrder(Analysis1TestingEntity.class);

        assertThat(findTypesMatching("text", "token_inserted_by_index_analysis_2"))
                .containsExactlyInAnyOrder(Analysis2TestingEntity.class);

        return "OK";
    }

    public List<Class<?>> findTypesMatching(@QueryParam String field, @QueryParam String term) {
        try (var session = searchMapping.createSession()) {
            return session.search(AnalysisTestingEntityBase.class)
                    .<Class<?>> select(f -> f.composite().from(f.entityReference()).as(EntityReference::type))
                    .where(f -> f.match().field(field).matching(term).skipAnalysis())
                    .fetchAllHits();
        }
    }
}
