package io.quarkus.hibernate.orm.config.properties;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.hibernate.FlushMode;
import org.hibernate.Session;
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
            .overrideConfigKey("quarkus.hibernate-orm.\"overrides\".flush.mode", "always");

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

}
