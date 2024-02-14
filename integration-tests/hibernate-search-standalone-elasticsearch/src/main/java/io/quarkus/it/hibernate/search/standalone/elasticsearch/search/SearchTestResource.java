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

@Path("/test/search")
public class SearchTestResource {

    @Inject
    SearchMapping searchMapping;

    @PUT
    @Path("/init-data")
    @Transactional
    public void initData() {
        try (var searchSession = searchMapping.createSession()) {
            createPerson(searchSession, 0, "John Irving", "Burlington");
            createPerson(searchSession, 1, "David Lodge", "London");
            createPerson(searchSession, 2, "Paul Auster", "New York");
            createPerson(searchSession, 3, "John Grisham", "Oxford");
        }
    }

    @GET
    @Path("/search")
    @Produces(MediaType.TEXT_PLAIN)
    public String testSearch() {
        try (var searchSession = searchMapping.createSession()) {
            List<EntityReference> person = searchSession.search(Person.class)
                    .selectEntityReference()
                    .where(f -> f.match().field("name").matching("john"))
                    .sort(f -> f.field("name_sort"))
                    .fetchHits(20);

            assertEquals(2, person.size());
            assertEquals(3L, person.get(0).id());
            assertEquals(0L, person.get(1).id());

            person = searchSession.search(Person.class)
                    .selectEntityReference()
                    .where(f -> f.match().field("address.city").matching("london"))
                    .sort(f -> f.field("name_sort"))
                    .fetchHits(20);

            assertEquals(1, person.size());
            assertEquals(1L, person.get(0).id());

            assertEquals(4, searchSession.search(Person.class)
                    .where(f -> f.matchAll())
                    .fetchTotalHitCount());
        }

        return "OK";
    }

    @GET
    @Path("/search-projection")
    @Produces(MediaType.TEXT_PLAIN)
    public String testSearchWithProjection() {
        try (var searchSession = searchMapping.createSession()) {
            assertThat(searchSession.search(Person.class)
                    .select(PersonDTO.class)
                    .where(f -> f.match().field("name").matching("john"))
                    .sort(f -> f.field("name_sort"))
                    .fetchHits(20))
                    .usingRecursiveFieldByFieldElementComparator()
                    .containsExactly(
                            new PersonDTO(3, "John Grisham", new AddressDTO("Oxford")),
                            new PersonDTO(0, "John Irving", new AddressDTO("Burlington")));
        }

        return "OK";
    }

    private void createPerson(SearchSession searchSession, long id, String name, String city) {
        searchSession.indexingPlan().add(new Person(id, name, new Address(city)));
    }
}
