package io.quarkus.hibernate.orm.config.properties;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.hibernate.orm.config.properties.defaultpu.MyEntityForDefaultPU;
import io.quarkus.hibernate.orm.config.properties.overridespu.MyEntityForOverridesPU;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Tests that configuration properties set in Quarkus are translated to the right key and value in Hibernate ORM.
 */
public class ConfigPropertiesTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .addPackage(MyEntityForDefaultPU.class.getPackage())
                    .addPackage(MyEntityForOverridesPU.class.getPackage()))
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.hibernate-orm.packages", MyEntityForDefaultPU.class.getPackageName())
            .overrideConfigKey("quarkus.hibernate-orm.\"overrides\".packages", MyEntityForOverridesPU.class.getPackageName())
            .overrideConfigKey("quarkus.hibernate-orm.\"overrides\".datasource", "<default>")
            // Overrides to test that Quarkus configuration properties are taken into account
            .overrideConfigKey("quarkus.hibernate-orm.\"overrides\".flush.mode", "always")
            .overrideConfigKey("quarkus.hibernate-orm.\"overrides\".schema-management.extra-physical-table-types",
                    "MATERIALIZED VIEW,FOREIGN TABLE")
            .overrideConfigKey("quarkus.hibernate-orm.\"overrides\".mapping.duration.preferred-jdbc-type", "INTERVAL_SECOND")
            .overrideConfigKey("quarkus.hibernate-orm.\"overrides\".mapping.instant.preferred-jdbc-type", "INSTANT")
            .overrideConfigKey("quarkus.hibernate-orm.\"overrides\".mapping.boolean.preferred-jdbc-type", "BIT")
            .overrideConfigKey("quarkus.hibernate-orm.\"overrides\".mapping.uuid.preferred-jdbc-type", "CHAR");

    @Inject
    Session sessionForDefaultPU;

    @Inject
    @PersistenceUnit("overrides")
    Session sessionForOverridesPU;

    @Test
    @Transactional
    public void propertiesAffectingSession() {
        assertThat(sessionForDefaultPU.getHibernateFlushMode()).isEqualTo(FlushMode.AUTO);
        assertThat(sessionForOverridesPU.getHibernateFlushMode()).isEqualTo(FlushMode.ALWAYS);
    }

    @Test
    @Transactional
    public void extraPhysicalTableTypes() {
        // Access the configuration through the SessionFactory properties
        Object extraPhysicalTableTypes = sessionForOverridesPU.getSessionFactory()
                .getProperties()
                .get(AvailableSettings.EXTRA_PHYSICAL_TABLE_TYPES);

        assertThat(extraPhysicalTableTypes).isNotNull();
        assertThat(extraPhysicalTableTypes).isInstanceOf(String.class);

        String tableTypesStr = (String) extraPhysicalTableTypes;
        String[] tableTypes = tableTypesStr.split(",");
        assertThat(tableTypes).containsExactly("MATERIALIZED VIEW", "FOREIGN TABLE");
    }

    @Test
    @Transactional
    void shouldMapHibernateOrmConfigPersistenceUnitMappingDurationProperties() {
        // given
        var preferredJdbcType = sessionForOverridesPU.getSessionFactory()
                .getProperties()
                .get(AvailableSettings.PREFERRED_DURATION_JDBC_TYPE);

        // when - then
        assertThat(preferredJdbcType).isEqualTo("INTERVAL_SECOND");
    }

    @Test
    @Transactional
    void shouldMapHibernateOrmConfigPersistenceUnitMappingPreferredTypesProperties() {
        // given
        var instantPreferredJdbcType = sessionForOverridesPU.getSessionFactory()
                .getProperties()
                .get(AvailableSettings.PREFERRED_INSTANT_JDBC_TYPE);

        var booleanPreferredJdbcType = sessionForOverridesPU.getSessionFactory()
                .getProperties()
                .get(AvailableSettings.PREFERRED_BOOLEAN_JDBC_TYPE);

        var UUIDPreferredJdbcType = sessionForOverridesPU.getSessionFactory()
                .getProperties()
                .get(AvailableSettings.PREFERRED_UUID_JDBC_TYPE);

        // when - then
        assertThat(instantPreferredJdbcType).isEqualTo("INSTANT");
        assertThat(booleanPreferredJdbcType).isEqualTo("BIT");
        assertThat(UUIDPreferredJdbcType).isEqualTo("CHAR");
    }
}
