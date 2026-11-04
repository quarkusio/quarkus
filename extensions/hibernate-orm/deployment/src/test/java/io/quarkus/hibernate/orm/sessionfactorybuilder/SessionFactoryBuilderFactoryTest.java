package io.quarkus.hibernate.orm.sessionfactorybuilder;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import org.hibernate.SessionFactory;
import org.hibernate.boot.spi.SessionFactoryBuilderFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.sessionfactorybuilder.CustomSessionFactoryBuilderFactory.CustomSessionFactory;
import io.quarkus.test.QuarkusExtensionTest;

public class SessionFactoryBuilderFactoryTest {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar
                    .addClass(MyEntity.class)
                    .addClass(CustomSessionFactoryBuilderFactory.class)
                    .addAsServiceProvider(SessionFactoryBuilderFactory.class, CustomSessionFactoryBuilderFactory.class));

    @Inject
    SessionFactory sessionFactory;

    @Inject
    EntityManager entityManager;

    @Test
    public void testCustomSessionFactory() {
        SessionFactoryImplementor sessionFactoryImplementor = sessionFactory.unwrap(SessionFactoryImplementor.class);
        assertThat(sessionFactoryImplementor).isInstanceOf(CustomSessionFactory.class);
        // The custom builder delegates to the default one, which holds the options configured by Quarkus
        assertThat(sessionFactoryImplementor.getSessionFactoryOptions().isJtaTransactionAccessEnabled()).isFalse();
    }

    @Test
    @Transactional
    public void testPersistAndFind() {
        MyEntity entity = new MyEntity();
        entity.setId(1L);
        entity.setName("some_name");
        entityManager.persist(entity);
        entityManager.flush();
        entityManager.clear();

        assertThat(entityManager.find(MyEntity.class, 1L).getName()).isEqualTo("some_name");
    }
}
