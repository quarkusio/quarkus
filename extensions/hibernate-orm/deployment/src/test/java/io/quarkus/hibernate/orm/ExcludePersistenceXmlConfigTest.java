package io.quarkus.hibernate.orm;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.hibernate.orm.enhancer.Address;
import io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil;
import io.quarkus.test.QuarkusUnitTest;

public class ExcludePersistenceXmlConfigTest {

    //The system property used by the Hibernate ORM extension to disable parsing of persistence.xml resources:
    private static String SKIP_PARSE_PERSISTENCE_XML = "SKIP_PARSE_PERSISTENCE_XML";

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setBeforeAllCustomizer(() -> System.setProperty(SKIP_PARSE_PERSISTENCE_XML, "true"))
            .setAfterAllCustomizer(() -> System.getProperties().remove(SKIP_PARSE_PERSISTENCE_XML))
            .withApplicationRoot((jar) -> jar
                    .addClass(Address.class)
                    .addAsManifestResource("META-INF/some-persistence.xml", "persistence.xml")
                    .addAsResource("application.properties"));

    @Inject
    EntityManager entityManager;

    @Inject
    Instance<EntityManager> entityManagers;

    @Test
    public void testPersistenceAndConfigTest() {
        // We have an entity manager
        Assertions.assertNotNull(entityManager);
        // We have exactly one entity manager
        Assertions.assertEquals(false, entityManagers.isAmbiguous());
        Arc.container().requestContext().activate();
        try {
            // it is the default entity manager from application.properties, not templatePU from the persistence.xml
            Assertions.assertEquals(PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME,
                    entityManager.getEntityManagerFactory().getProperties().get("hibernate.ejb.persistenceUnitName"));
        } finally {
            Arc.container().requestContext().deactivate();
        }
    }

}
