package io.quarkus.it.hibernate.search.orm.elasticsearch.mapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.util.common.SearchException;

@Path("/test/mapping")
public class MappingTestResource {

    @Inject
    EntityManager entityManager;

    @PUT
    @Path("/init-data")
    @Transactional
    public void initData() {
        entityManager.persist(new MappingTestingApplicationBeanEntity("text"));
        entityManager.persist(new MappingTestingDependentBeanEntity("text"));
        entityManager.persist(new MappingTestingClassEntity("text"));
        entityManager.persist(new MappingTestingSearchExtensionEntity("text"));
    }

    @GET
    @Path("/mapping-property")
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public String testMappingConfiguredProperty() {
        SearchSession searchSession = Search.session(entityManager);
        assertSearch(searchSession, MappingTestingApplicationBeanEntity.class);
        assertSearch(searchSession, MappingTestingDependentBeanEntity.class);
        assertSearch(searchSession, MappingTestingClassEntity.class);
        // since the property overrides this mapper from the extension:
        assertThatThrownBy(() -> assertSearch(searchSession, MappingTestingSearchExtensionEntity.class))
                .isInstanceOf(SearchException.class)
                .hasMessageContainingAll("No matching indexed entity types for classes",
                        MappingTestingSearchExtensionEntity.class.getName());
        return "OK";
    }

    @GET
    @Path("/mapping-extension")
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public String testMappingConfiguredExtension() {
        SearchSession searchSession = Search.session(entityManager);

        assertThatThrownBy(() -> assertSearch(searchSession, MappingTestingApplicationBeanEntity.class))
                .isInstanceOf(SearchException.class)
                .hasMessageContainingAll("No matching indexed entity types for classes",
                        MappingTestingApplicationBeanEntity.class.getName());
        assertThatThrownBy(() -> assertSearch(searchSession, MappingTestingDependentBeanEntity.class))
                .isInstanceOf(SearchException.class)
                .hasMessageContainingAll("No matching indexed entity types for classes",
                        MappingTestingDependentBeanEntity.class.getName());
        assertThatThrownBy(() -> assertSearch(searchSession, MappingTestingClassEntity.class))
                .isInstanceOf(SearchException.class)
                .hasMessageContainingAll("No matching indexed entity types for classes",
                        MappingTestingClassEntity.class.getName());

        assertSearch(searchSession, MappingTestingSearchExtensionEntity.class);
        return "OK";
    }

    private void assertSearch(SearchSession searchSession, Class<?> type) {
        assertThat(searchSession.search(type)
                .select(f -> f.field("text"))
                .where(f -> f.match().field("text").matching("text"))
                .fetchAllHits())
                .hasSize(1)
                .containsOnly("text");
    }
}
