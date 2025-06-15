package io.quarkus.hibernate.orm.xml.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil;
import io.quarkus.test.QuarkusUnitTest;

public class ExcludePersistenceXmlConfigUsingApplicationPropertiesTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClass(MyEntity.class)
                    .addAsManifestResource("META-INF/some-persistence.xml", "persistence.xml"))
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.hibernate-orm.persistence-xml.ignore", "true");

    @Inject
    EntityManager entityManager;

    @Inject
    Instance<EntityManager> entityManagers;

    @Test
    public void puIsFromApplicationProperties() {
        // We have an entity manager
        Assertions.assertNotNull(entityManager);
        // We have exactly one entity manager
        Assertions.assertEquals(false, entityManagers.isAmbiguous());
        Arc.container().requestContext().activate();
        try {
            // it is the default entity manager from application.properties, not templatePU from the persistence.xml
            Assertions.assertEquals(PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME,
                    entityManager.getEntityManagerFactory().getProperties()
                            .get(org.hibernate.cfg.AvailableSettings.PERSISTENCE_UNIT_NAME));
        } finally {
            Arc.container().requestContext().deactivate();
        }
    }

    @Test
    @Transactional
    public void smokeTest() {
        MyEntity persistedEntity = new MyEntity("someName");
        entityManager.persist(persistedEntity);
        entityManager.flush();
        entityManager.clear();
        MyEntity retrievedEntity = entityManager.find(MyEntity.class, persistedEntity.id);
        assertThat(retrievedEntity.name).isEqualTo(persistedEntity.name);
    }

}
