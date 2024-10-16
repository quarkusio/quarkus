package io.quarkus.it.hibernate.search.standalone.elasticsearch.propertyaccess;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.assertj.core.util.TriFunction;
import org.hibernate.search.mapper.pojo.common.spi.PojoEntityReference;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;

@Path("/test/property-access")
public class PropertyAccessTestResource {

    @Inject
    SearchMapping searchMapping;

    @GET
    @Path("/private-field")
    @Produces(MediaType.TEXT_PLAIN)
    public String testPrivateFieldAccess() {
        return testAccess(PrivateFieldAccessEntity.class, PrivateFieldAccessEntity::new);
    }

    @GET
    @Path("/public-field")
    @Produces(MediaType.TEXT_PLAIN)
    public String testPublicFieldAccess() {
        return testAccess(PublicFieldAccessEntity.class, PublicFieldAccessEntity::new);
    }

    @GET
    @Path("/method")
    @Produces(MediaType.TEXT_PLAIN)
    public String testMethodAccess() {
        return testAccess(MethodAccessEntity.class, MethodAccessEntity::new);
    }

    @GET
    @Path("/record-field")
    @Produces(MediaType.TEXT_PLAIN)
    public String testRecordFieldAccess() {
        return testAccess(RecordFieldAccessEntity.class, RecordFieldAccessEntity::new);
    }

    private <T> String testAccess(Class<T> clazz, TriFunction<Long, String, String, T> constructor) {
        long id = 1L;
        String value1 = "foo1";
        String value2 = "foo2";

        try (var session = searchMapping.createSession()) {
            assertThat(session.search(clazz)
                    .selectEntityReference()
                    .where(f -> f.and(
                            f.match().field("property").matching(value1),
                            f.match().field("otherProperty").matching(value2)))
                    .fetchAllHits())
                    .isEmpty();
        }

        try (var session = searchMapping.createSession()) {
            session.indexingPlan().add(constructor.apply(id, value1, value2));
        }

        try (var session = searchMapping.createSession()) {
            assertThat(session.search(clazz)
                    .selectEntityReference()
                    .where(f -> f.and(
                            f.match().field("property").matching(value1),
                            f.match().field("otherProperty").matching(value2)))
                    .fetchAllHits())
                    .containsExactly(PojoEntityReference.withDefaultName(clazz, id));
        }

        return "OK";
    }

}
