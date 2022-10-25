package io.quarkus.hibernate.orm.xml.persistence;

import static org.hibernate.cfg.AvailableSettings.PERSISTENCE_UNIT_NAME;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.hibernate.orm.SmokeTestUtils;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
public class PersistenceXmlTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(SmokeTestUtils.class)
                    .addClass(MyEntity.class)
                    .addAsManifestResource("META-INF/some-persistence.xml", "persistence.xml")
                    .addAsResource("application-datasource-only.properties", "application.properties"));

    @Inject
    EntityManager entityManager;

    @Test
    public void puIsFromPersistenceXml() {
        Arc.container().requestContext().activate();
        try {
            // the PU is templatePU from the persistence.xml, not the default entity manager from application.properties
            Assertions.assertEquals("templatePU",
                    entityManager.getEntityManagerFactory().getProperties().get(PERSISTENCE_UNIT_NAME));
        } finally {
            Arc.container().requestContext().deactivate();
        }
    }

    @Test
    @Transactional
    public void smokeTest() {
        SmokeTestUtils.testSimplePersistRetrieveUpdateDelete(entityManager,
                MyEntity.class, MyEntity::new,
                e -> e.id, (e, value) -> e.name = value, e -> e.name);
    }

}
