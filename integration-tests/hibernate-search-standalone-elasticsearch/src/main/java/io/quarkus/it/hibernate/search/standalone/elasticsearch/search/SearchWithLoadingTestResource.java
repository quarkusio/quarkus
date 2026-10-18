package io.quarkus.it.hibernate.search.standalone.elasticsearch.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.hibernate.search.engine.common.EntityReference;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;

import io.quarkus.it.hibernate.search.standalone.elasticsearch.search.stub.DatastoreConnectionStub;
import io.quarkus.it.hibernate.search.standalone.elasticsearch.search.stub.DatastoreStub;

@Path("/test/search-with-loading")
public class SearchWithLoadingTestResource {

    @Inject
    SearchMapping searchMapping;

    @Inject
    DatastoreStub datastore;

    @PUT
    @Path("/init-data")
    @Transactional
    public void initData() {
        try (var connection = datastore.connect();
                var searchSession = searchMapping.createSession()) {
            createLoadablePerson(connection, searchSession, 0L, "John Irving");
            createLoadablePerson(connection, searchSession, 1L, "David Lodge");
            createLoadablePerson(connection, searchSession, 2L, "Paul Auster");
            createLoadablePerson(connection, searchSession, 3L, "John Grisham");

            // Add many other entities, so that mass indexing has something to do.
            // DO NOT REMOVE, it's important to have many entities to fully test mass indexing.
            for (long i = 4L; i < 2000L; i++) {
                createLoadablePerson(connection, searchSession, i, "Other Person #" + i);
            }
        }
    }

    @GET
    @Path("/search")
    @Produces(MediaType.TEXT_PLAIN)
    public String testSearch() {
        try (var connection = datastore.connect();
                var searchSession = searchMapping.createSessionWithOptions()
                        .loading(o -> o.context(DatastoreConnectionStub.class, connection))
                        .build()) {
            assertThat(searchSession.search(LoadablePerson.class)
                    .where(f -> f.match().field("name").matching("john"))
                    .sort(f -> f.field("name_sort"))
                    .fetchHits(20))
                    .usingRecursiveFieldByFieldElementComparator()
                    .containsExactlyElementsOf(connection.loadEntitiesByIdInSameOrder(LoadablePerson.class, List.of(3L, 0L)));
        }

        return "OK";
    }

    @PUT
    @Path("/purge")
    @Produces(MediaType.TEXT_PLAIN)
    public String testPurge() {
        searchMapping.scope(LoadablePerson.class).workspace().purge();

        return "OK";
    }

    @PUT
    @Path("/refresh")
    @Produces(MediaType.TEXT_PLAIN)
    public String testRefresh() {
        searchMapping.scope(LoadablePerson.class).workspace().refresh();

        return "OK";
    }

    @GET
    @Path("/search-empty")
    @Produces(MediaType.TEXT_PLAIN)
    public String testSearchEmpty() {
        try (var searchSession = searchMapping.createSession()) {
            List<EntityReference> person = searchSession.search(LoadablePerson.class)
                    .selectEntityReference()
                    .where(f -> f.matchAll())
                    .fetchHits(20);
            assertEquals(0, person.size());
        }

        return "OK";
    }

    @PUT
    @Path("/mass-indexer")
    @Produces(MediaType.TEXT_PLAIN)
    public String testMassIndexer() throws InterruptedException {
        searchMapping.scope(LoadablePerson.class).massIndexer().startAndWait();

        return "OK";
    }

    private void createLoadablePerson(DatastoreConnectionStub connection, SearchSession searchSession,
            long id, String name) {
        var person = new LoadablePerson(id, name);
        connection.put(id, person);
        searchSession.indexingPlan().add(person);
    }
}
