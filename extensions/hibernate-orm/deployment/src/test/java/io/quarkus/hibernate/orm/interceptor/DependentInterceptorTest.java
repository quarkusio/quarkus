package io.quarkus.hibernate.orm.interceptor;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.enterprise.context.Dependent;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.transaction.UserTransaction;

import org.hibernate.EmptyInterceptor;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.type.Type;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.ClientProxy;
import io.quarkus.hibernate.orm.PersistenceUnitExtension;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.test.QuarkusUnitTest;

public class DependentInterceptorTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(MyEntity.class)
                    .addClass(DependentInterceptor.class))
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
        DependentInterceptor.instances.clear();
        DependentInterceptor.loadedIds.clear();
    }

    @Test
    public void testTransactionScopedSession() throws Exception {
        assertThat(DependentInterceptor.instances).isEmpty();
        assertThat(DependentInterceptor.loadedIds).isEmpty();

        transaction.begin();
        session.find(MyEntity.class, 0);
        transaction.commit();
        assertThat(DependentInterceptor.instances).hasSize(1); // One instance per session
        assertThat(DependentInterceptor.loadedIds).containsExactly(0);

        transaction.begin();
        session.find(MyEntity.class, 1);
        session.find(MyEntity.class, 2);
        transaction.commit();
        assertThat(DependentInterceptor.instances).hasSize(2); // One instance per session
        assertThat(DependentInterceptor.loadedIds).containsExactly(0, 1, 2);
    }

    @Test
    public void testManualSessionNoTransaction() {
        assertThat(DependentInterceptor.instances).isEmpty();
        assertThat(DependentInterceptor.loadedIds).isEmpty();

        try (Session manualSession = sessionFactory.openSession()) {
            manualSession.find(MyEntity.class, 0);
        }
        assertThat(DependentInterceptor.instances).hasSize(1); // One instance per session
        assertThat(DependentInterceptor.loadedIds).containsExactly(0);

        try (Session manualSession = sessionFactory.openSession()) {
            manualSession.find(MyEntity.class, 1);
            manualSession.find(MyEntity.class, 2);
        }
        assertThat(DependentInterceptor.instances).hasSize(2); // One instance per session
        assertThat(DependentInterceptor.loadedIds).containsExactly(0, 1, 2);
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
    @Dependent
    public static class DependentInterceptor extends EmptyInterceptor {
        private static final List<DependentInterceptor> instances = Collections.synchronizedList(new ArrayList<>());
        private static final List<Object> loadedIds = Collections.synchronizedList(new ArrayList<>());

        public DependentInterceptor() {
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
