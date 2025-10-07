package io.quarkus.it.jpa.preferredhibernatetypesoverride;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.hibernate.Session;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.StandardBasicTypes;

import io.quarkus.hibernate.orm.PersistenceUnit;

@Path("/overridden-preferred-types")
@ApplicationScoped
public class OverriddenPreferredTypesResource {

    @Inject
    @PersistenceUnit("overridden-types")
    Session session;

    @GET
    @Path("/test-successful-persistence")
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public String testSuccessfulPersistence() {
        var entity = persistEntity();

        assertThat(findUsingNativeQuery()).contains(entity.id);

        return "OK";
    }

    @GET
    @Path("/test-successful-override")
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public String testSuccessfulPreferredTypesOverride() {
        persistEntity();

        var metamodel = session.getFactory()
                .unwrap(SessionFactoryImplementor.class)
                .getMappingMetamodel()
                .findEntityDescriptor(EntityWithOverridablePreferredTypes.class);

        assertThat(metamodel.getIdentifierMapping().getSingleJdbcMapping().getJdbcType()
                .getDefaultSqlTypeCode())
                .isEqualTo(SqlTypes.CHAR);
        assertThat(metamodel.getAttributeMapping(metamodel.getPropertyIndex("createdAt")).getSingleJdbcMapping().getJdbcType()
                .getDefaultSqlTypeCode())
                .isEqualTo(SqlTypes.INSTANT);

        assertThat(metamodel.getAttributeMapping(metamodel.getPropertyIndex("overridenDuration")).getSingleJdbcMapping()
                .getJdbcType()
                .getDefaultSqlTypeCode())
                .isEqualTo(SqlTypes.INTERVAL_SECOND);

        // Cannot detect BIT from the metamodel because it's handled as Boolean at runtime
        // See https://github.com/hibernate/hibernate-orm/blob/018b8eeda3627e114ec25bd48407ccb9c47564ce/hibernate-core/src/main/java/org/hibernate/type/descriptor/jdbc/BooleanJdbcType.java#L61-L66
        assertThat(metamodel.getAttributeMapping(metamodel.getPropertyIndex("isPersisted")).getSingleJdbcMapping().getJdbcType()
                .getDefaultSqlTypeCode())
                .isEqualTo(SqlTypes.BOOLEAN);

        return "OK";
    }

    private EntityWithOverridablePreferredTypes persistEntity() {
        var entity = new EntityWithOverridablePreferredTypes();
        entity.isPersisted = true;
        entity.createdAt = Instant.now();
        entity.overridenDuration = Duration.ofDays(1);

        session.persist(entity);
        session.flush();
        session.clear();

        return entity;
    }

    private List<UUID> findUsingNativeQuery() {
        return session.createNativeQuery(
                """
                        SELECT id FROM %s WHERE isPersisted = :isPersisted
                        """.formatted(EntityWithOverridablePreferredTypes.NAME))
                .addScalar("id", StandardBasicTypes.UUID_CHAR)
                .setParameter("isPersisted", true)
                .getResultList();
    }
}
