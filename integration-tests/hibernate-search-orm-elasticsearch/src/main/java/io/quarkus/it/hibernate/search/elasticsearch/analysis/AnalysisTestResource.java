package io.quarkus.it.hibernate.search.elasticsearch.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.common.EntityReference;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.jboss.resteasy.annotations.jaxrs.QueryParam;

@Path("/test/analysis")
public class AnalysisTestResource {

    @Inject
    EntityManager entityManager;

    @PUT
    @Path("/init-data")
    @Transactional
    public void initData() {
        entityManager.persist(new Analysis0TestingEntity("irrelevant"));
        entityManager.persist(new Analysis1TestingEntity("irrelevant"));
        entityManager.persist(new Analysis2TestingEntity("irrelevant"));
        entityManager.persist(new Analysis3TestingEntity("irrelevant"));
        entityManager.persist(new Analysis4TestingEntity("irrelevant"));
        entityManager.persist(new Analysis5TestingEntity("irrelevant"));
    }

    @GET
    @Path("/analysis-configured")
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public String testAnalysisConfigured() {
        assertThat(findTypesMatching("text", "token_inserted_by_backend_analysis"))
                .containsExactlyInAnyOrder(Analysis0TestingEntity.class);

        assertThat(findTypesMatching("text", "token_inserted_by_index_analysis_1"))
                .containsExactlyInAnyOrder(Analysis1TestingEntity.class);

        assertThat(findTypesMatching("text", "token_inserted_by_index_analysis_2"))
                .containsExactlyInAnyOrder(Analysis2TestingEntity.class);

        assertThat(findTypesMatching("text", "token_inserted_by_index_analysis_3"))
                .containsExactlyInAnyOrder(Analysis3TestingEntity.class);

        assertThat(findTypesMatching("text", "token_inserted_by_index_analysis_4"))
                .containsExactlyInAnyOrder(Analysis4TestingEntity.class);

        assertThat(findTypesMatching("text", "token_inserted_by_index_analysis_5"))
                .containsExactlyInAnyOrder(Analysis5TestingEntity.class);

        return "OK";
    }

    public List<Class<?>> findTypesMatching(@QueryParam String field, @QueryParam String term) {
        SearchSession searchSession = Search.session(entityManager);
        return searchSession.search(AnalysisTestingEntityBase.class)
                .<Class<?>> select(f -> f.composite(EntityReference::type, f.entityReference()))
                .where(f -> f.match().field(field).matching(term).skipAnalysis())
                .fetchAllHits();
    }
}
