package io.quarkus.hibernate.orm.interceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.transaction.TransactionScoped;
import javax.transaction.UserTransaction;

import org.hibernate.EmptyInterceptor;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.type.Type;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.ClientProxy;
import io.quarkus.hibernate.orm.PersistenceUnitExtension;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.test.QuarkusUnitTest;

public class TransactionScopedInterceptorTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(MyEntity.class)
                    .addClass(TransactionScopedInterceptor.class))
            .withConfigurationResource("application.properties");

    @Inject
    SessionFactory sessionFactory;

    @Inject
    Session session;

    @Inject
    UserTransaction transaction;

    public void initData(@Observes StartupEvent event) throws Exception {
        transaction.begin();
        for (int i = 0; i < 3; i++) {
            MyEntity entity = new MyEntity(i);
            session.persist(entity);
        }
        transaction.commit();
    }

    @BeforeEach
    public void clearInterceptor() {
        TransactionScopedInterceptor.instances.clear();
        TransactionScopedInterceptor.loadedIds.clear();
    }

    @Test
    public void testTransactionScopedSession() throws Exception {
        assertThat(TransactionScopedInterceptor.instances).isEmpty();
        assertThat(TransactionScopedInterceptor.loadedIds).isEmpty();

        transaction.begin();
        session.find(MyEntity.class, 0);
        transaction.commit();
        assertThat(TransactionScopedInterceptor.instances).hasSize(1); // One instance per session
        assertThat(TransactionScopedInterceptor.loadedIds).containsExactly(0);

        transaction.begin();
        session.find(MyEntity.class, 1);
        session.find(MyEntity.class, 2);
        transaction.commit();
        assertThat(TransactionScopedInterceptor.instances).hasSize(2); // One instance per session
        assertThat(TransactionScopedInterceptor.loadedIds).containsExactly(0, 1, 2);
    }

    @Test
    public void testManualSessionNoTransaction() {
        assertThat(TransactionScopedInterceptor.instances).isEmpty();
        assertThat(TransactionScopedInterceptor.loadedIds).isEmpty();

        // If the interceptor is transaction-scoped, transaction-less sessions cannot be used anymore.
        // People should probably avoid transaction-less sessions anyway,
        // and they don't *have* to make their interceptor transaction-scoped;
        // they could use @Dependent.
        assertThatThrownBy(() -> {
            try (Session manualSession = sessionFactory.openSession()) {
                manualSession.find(MyEntity.class, 0);
            }
        })
                .isInstanceOf(javax.enterprise.context.ContextNotActiveException.class);

        assertThat(TransactionScopedInterceptor.instances).isEmpty();
        assertThat(TransactionScopedInterceptor.loadedIds).isEmpty();
    }

    @Entity(name = "myentity")
    @Table
    public static class MyEntity {

        @Id
        public Integer id;

        public MyEntity() {
        }

        public MyEntity(int id) {
            this.id = id;
        }
    }

    @PersistenceUnitExtension
    @TransactionScoped
    public static class TransactionScopedInterceptor extends EmptyInterceptor {
        private static final List<TransactionScopedInterceptor> instances = Collections.synchronizedList(new ArrayList<>());
        private static final List<Object> loadedIds = Collections.synchronizedList(new ArrayList<>());

        public TransactionScopedInterceptor() {
            if (!ClientProxy.class.isAssignableFrom(getClass())) { // Disregard CDI proxies extending this class
                instances.add(this);
            }
        }

        @Override
        public boolean onLoad(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
            loadedIds.add(id);
            return false;
        }
    }
}
