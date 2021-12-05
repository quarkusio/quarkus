package io.quarkus.hibernate.orm.singlepersistenceunit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;

import org.hibernate.Session;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class SinglePersistenceUnitResourceInjectionSessionTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(DefaultEntity.class)
                    .addAsResource("application.properties"));

    @PersistenceContext
    Session session;

    @Test
    @Transactional
    public void test() {
        DefaultEntity entity = new DefaultEntity("gsmet");
        session.persist(entity);

        DefaultEntity savedEntity = session.get(DefaultEntity.class, entity.getId());
        assertEquals(entity.getName(), savedEntity.getName());
    }

}
