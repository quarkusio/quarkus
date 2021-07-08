package io.quarkus.hibernate.reactive;

import static org.assertj.core.api.Assertions.assertThat;

import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.reactive.mutiny.Mutiny;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class NoJtaTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(MyEntity.class)
                    .addAsResource("application.properties"));

    @Inject
    SessionFactory sessionFactory; // This is an ORM SessionFactory, but it's backing Hibernate Reactive.

    @Inject
    Mutiny.SessionFactory factory;

    @Test
    @ActivateRequestContext
    public void test() {
        ServiceRegistryImplementor serviceRegistry = sessionFactory.unwrap(SessionFactoryImplementor.class)
                .getServiceRegistry();

        // Two assertions are necessary, because these values are influenced by separate configuration
        assertThat(serviceRegistry.getService(JtaPlatform.class).retrieveTransactionManager()).isNull();
        assertThat(serviceRegistry.getService(TransactionCoordinatorBuilder.class).isJta()).isFalse();

        // Quick test to make sure HRX works
        MyEntity entity = new MyEntity("default");
        MyEntity retrievedEntity = factory.withTransaction((session, tx) -> session.persist(entity))
                .chain(() -> factory.withTransaction((session, tx) -> session.clear().find(MyEntity.class, entity.getId())))
                .await().indefinitely();
        assertThat(retrievedEntity)
                .isNotSameAs(entity)
                .returns(entity.getName(), MyEntity::getName);
    }

    @Entity
    public static class MyEntity {

        private long id;

        private String name;

        public MyEntity() {
        }

        public MyEntity(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + ":" + name;
        }

        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "defaultSeq")
        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
