package io.quarkus.hibernate.orm.multiplepersistenceunits;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.transaction.UserTransaction;

import org.hibernate.EmptyInterceptor;
import org.hibernate.Session;
import org.hibernate.type.Type;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.ClientProxy;
import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.hibernate.orm.PersistenceUnitExtension;
import io.quarkus.hibernate.orm.multiplepersistenceunits.model.config.DefaultEntity;
import io.quarkus.hibernate.orm.multiplepersistenceunits.model.config.inventory.Plane;
import io.quarkus.hibernate.orm.multiplepersistenceunits.model.config.user.User;
import io.quarkus.test.QuarkusUnitTest;

public class MultiplePersistenceUnitsInterceptorTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(DefaultEntity.class)
                    .addClass(User.class)
                    .addClass(Plane.class)
                    .addClass(MyDefaultPUInterceptor.class)
                    .addClass(MyInventoryPUInterceptor.class))
            .withConfigurationResource("application-multiple-persistence-units.properties");

    @Inject
    Session defaultSession;

    @Inject
    @PersistenceUnit("users")
    Session usersSession;

    @Inject
    @PersistenceUnit("inventory")
    Session inventorySession;

    @Inject
    UserTransaction transaction;

    private long defaultEntityId;
    private long userId;
    private long planeId;

    @BeforeEach
    public void initData() throws Exception {
        transaction.begin();
        DefaultEntity entity = new DefaultEntity("default");
        defaultSession.persist(entity);
        User user = new User("user");
        usersSession.persist(user);
        Plane plane = new Plane("plane");
        inventorySession.persist(plane);
        transaction.commit();
        defaultEntityId = entity.getId();
        userId = user.getId();
        planeId = plane.getId();
    }

    @Test
    public void test() throws Exception {
        assertThat(MyDefaultPUInterceptor.instances).hasSize(1);
        assertThat(MyInventoryPUInterceptor.instances).hasSize(1);
        assertThat(MyDefaultPUInterceptor.loadedIds).isEmpty();
        assertThat(MyInventoryPUInterceptor.loadedIds).isEmpty();

        transaction.begin();
        defaultSession.find(DefaultEntity.class, defaultEntityId);
        transaction.commit();
        assertThat(MyDefaultPUInterceptor.loadedIds).containsExactly(defaultEntityId);
        assertThat(MyInventoryPUInterceptor.loadedIds).isEmpty();

        transaction.begin();
        usersSession.find(User.class, userId);
        transaction.commit();
        assertThat(MyDefaultPUInterceptor.loadedIds).containsExactly(defaultEntityId);
        assertThat(MyInventoryPUInterceptor.loadedIds).isEmpty();

        transaction.begin();
        inventorySession.find(Plane.class, planeId);
        transaction.commit();
        assertThat(MyDefaultPUInterceptor.loadedIds).containsExactly(defaultEntityId);
        assertThat(MyInventoryPUInterceptor.loadedIds).containsExactly(planeId);

        // No new instance: it's application-scoped
        assertThat(MyDefaultPUInterceptor.instances).hasSize(1);
        assertThat(MyInventoryPUInterceptor.instances).hasSize(1);
    }

    @PersistenceUnitExtension
    public static class MyDefaultPUInterceptor extends EmptyInterceptor {
        private static final List<MyDefaultPUInterceptor> instances = Collections.synchronizedList(new ArrayList<>());
        private static final List<Object> loadedIds = Collections.synchronizedList(new ArrayList<>());

        public MyDefaultPUInterceptor() {
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

    @PersistenceUnitExtension("inventory")
    public static class MyInventoryPUInterceptor extends EmptyInterceptor {
        private static final List<MyInventoryPUInterceptor> instances = Collections.synchronizedList(new ArrayList<>());
        private static final List<Object> loadedIds = Collections.synchronizedList(new ArrayList<>());

        public MyInventoryPUInterceptor() {
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
