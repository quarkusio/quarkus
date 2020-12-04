package io.quarkus.hibernate.orm.singlepersistenceunit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class SinglePersistenceUnitResourceInjectionEntityManagerTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(DefaultEntity.class)
                    .addAsResource("application.properties"));

    @PersistenceContext
    EntityManager entityManager;

    @Test
    @Transactional
    public void test() {
        DefaultEntity entity = new DefaultEntity("gsmet");
        entityManager.persist(entity);

        DefaultEntity savedEntity = entityManager.find(DefaultEntity.class, entity.getId());
        assertEquals(entity.getName(), savedEntity.getName());
    }

}
