package io.quarkus.hibernate.orm;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.hibernate.orm.enhancer.Address;
import io.quarkus.test.QuarkusUnitTest;

public class ExcludePersistenceXmlConfigTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(Address.class)
                    .addAsManifestResource("META-INF/some-persistence.xml", "persistence.xml")
                    .addAsResource("application-exclude-persistence-xml.properties", "application.properties"));

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
            Assertions.assertEquals("default",
                    entityManager.getEntityManagerFactory().getProperties().get("hibernate.ejb.persistenceUnitName"));
        } finally {
            Arc.container().requestContext().deactivate();
        }
    }

}
