package io.quarkus.hibernate.orm.lazyloading;

import static io.quarkus.hibernate.orm.TransactionTestUtils.inTransaction;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.UserTransaction;

import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.junit.jupiter.api.Test;

import io.quarkus.hibernate.orm.PersistenceUnitExtension;

public abstract class AbstractLazyBasicTest {

    @Inject
    EntityManager em;

    @Inject
    UserTransaction transaction;

    private final AccessDelegate delegate;
    private Long entityId;

    public AbstractLazyBasicTest(AccessDelegate delegate) {
        this.delegate = delegate;
    }

    @Test
    public void update_all_nullToNull() {
        initNull();
        // Updating lazy properties always results in updates, even if the value didn't change,
        // because we don't know of their previous value.
        StatementSpy.checkAtLeastOneUpdate(() -> inTransaction(transaction, () -> {
            delegate.updateAllProperties(em, entityId, null, null, null);
        }));
        inTransaction(transaction, () -> {
            delegate.testLazyLoadingAndPersistedValues(em, entityId, null, null, null);
        });
    }

    @Test
    public void update_allLazy_nullToNull() {
        initNull();
        // Updating lazy properties always results in updates, even if the value didn't change,
        // because we don't know of their previous value.
        StatementSpy.checkAtLeastOneUpdate(() -> inTransaction(transaction, () -> {
            delegate.updateAllLazyProperties(em, entityId, null, null);
        }));
        inTransaction(transaction, () -> {
            delegate.testLazyLoadingAndPersistedValues(em, entityId, null, null, null);
        });
    }

    @Test
    public void update_oneEager_nullToNull() {
        initNull();
        StatementSpy.checkNoUpdate(() -> inTransaction(transaction, () -> {
            delegate.updateOneEagerProperty(em, entityId, null);
        }));
        inTransaction(transaction, () -> {
            delegate.testLazyLoadingAndPersistedValues(em, entityId, null, null, null);
        });
    }

    @Test
    public void update_oneLazy_nullToNull() {
        initNull();
        // Updating lazy properties always results in updates, even if the value didn't change,
        // because we don't know of their previous value.
        StatementSpy.checkAtLeastOneUpdate(() -> inTransaction(transaction, () -> {
            delegate.updateOneLazyProperty(em, entityId, null);
        }));
        inTransaction(transaction, () -> {
            delegate.testLazyLoadingAndPersistedValues(em, entityId, null, null, null);
        });
    }

    @Test
    public void update_all_nullToNonNull() {
        initNull();
        StatementSpy.checkAtLeastOneUpdate(() -> inTransaction(transaction, () -> {
            delegate.updateAllProperties(em, entityId, "updated1", "updated2", "updated3");
        }));
        inTransaction(transaction, () -> {
            delegate.testLazyLoadingAndPersistedValues(em, entityId, "updated1", "updated2", "updated3");
        });
    }

    @Test
    public void update_allLazy_nullToNonNull() {
        initNull();
        StatementSpy.checkAtLeastOneUpdate(() -> inTransaction(transaction, () -> {
            delegate.updateAllLazyProperties(em, entityId, "updated1", "updated2");
        }));
        inTransaction(transaction, () -> {
            delegate.testLazyLoadingAndPersistedValues(em, entityId, null, "updated1", "updated2");
        });
    }

    @Test
    public void update_oneEager_nullToNonNull() {
        initNull();
        StatementSpy.checkAtLeastOneUpdate(() -> inTransaction(transaction, () -> {
            delegate.updateOneEagerProperty(em, entityId, "updated1");
        }));
        inTransaction(transaction, () -> {
            delegate.testLazyLoadingAndPersistedValues(em, entityId, "updated1", null, null);
        });
    }

    @Test
    public void update_oneLazy_nullToNonNull() {
        initNull();
        StatementSpy.checkAtLeastOneUpdate(() -> inTransaction(transaction, () -> {
            delegate.updateOneLazyProperty(em, entityId, "updated2");
        }));
        inTransaction(transaction, () -> {
            delegate.testLazyLoadingAndPersistedValues(em, entityId, null, "updated2", null);
        });
    }

    @Test
    public void update_all_nonNullToNonNull_differentValue() {
        initNonNull();
        StatementSpy.checkAtLeastOneUpdate(() -> inTransaction(transaction, () -> {
            delegate.updateAllProperties(em, entityId, "updated1", "updated2", "updated3");
        }));
        inTransaction(transaction, () -> {
            delegate.testLazyLoadingAndPersistedValues(em, entityId, "updated1", "updated2", "updated3");
        });
    }

    @Test
    public void update_all_nonNullToNonNull_sameValue() {
        initNonNull();
        // Updating lazy properties always results in updates, even if the value didn't change,
        // because we don't know of their previous value.
        StatementSpy.checkAtLeastOneUpdate(() -> inTransaction(transaction, () -> {
            delegate.updateAllProperties(em, entityId, "initial1", "initial2", "initial3");
        }));
        inTransaction(transaction, () -> {
            delegate.testLazyLoadingAndPersistedValues(em, entityId, "initial1", "initial2", "initial3");
        });
    }

    @Test
    public void update_allLazy_nonNullToNonNull_differentValue() {
        initNonNull();
        StatementSpy.checkAtLeastOneUpdate(() -> inTransaction(transaction, () -> {
            delegate.updateAllLazyProperties(em, entityId, "updated1", "updated2");
        }));
        inTransaction(transaction, () -> {
            delegate.testLazyLoadingAndPersistedValues(em, entityId, "initial1", "updated1", "updated2");
        });
    }

    @Test
    public void update_allLazy_nonNullToNonNull_sameValue() {
        initNonNull();
        // Updating lazy properties always results in updates, even if the value didn't change,
        // because we don't know of their previous value.
        StatementSpy.checkAtLeastOneUpdate(() -> inTransaction(transaction, () -> {
            delegate.updateAllLazyProperties(em, entityId, "initial2", "initial3");
        }));
        inTransaction(transaction, () -> {
            delegate.testLazyLoadingAndPersistedValues(em, entityId, "initial1", "initial2", "initial3");
        });
    }

    @Test
    public void update_oneEager_nonNullToNonNull_differentValue() {
        initNonNull();
        StatementSpy.checkAtLeastOneUpdate(() -> inTransaction(transaction, () -> {
            delegate.updateOneEagerProperty(em, entityId, "updated1");
        }));
        inTransaction(transaction, () -> {
            delegate.testLazyLoadingAndPersistedValues(em, entityId, "updated1", "initial2", "initial3");
        });
    }

    @Test
    public void update_oneEager_nonNullToNonNull_sameValue() {
        initNonNull();
        StatementSpy.checkNoUpdate(() -> inTransaction(transaction, () -> {
            delegate.updateOneEagerProperty(em, entityId, "initial1");
        }));
        inTransaction(transaction, () -> {
            delegate.testLazyLoadingAndPersistedValues(em, entityId, "initial1", "initial2", "initial3");
        });
    }

    @Test
    public void update_oneLazy_nonNullToNonNull_differentValue() {
        initNonNull();
        StatementSpy.checkAtLeastOneUpdate(() -> inTransaction(transaction, () -> {
            delegate.updateOneLazyProperty(em, entityId, "updated2");
        }));
        inTransaction(transaction, () -> {
            delegate.testLazyLoadingAndPersistedValues(em, entityId, "initial1", "updated2", "initial3");
        });
    }

    @Test
    public void update_oneLazy_nonNullToNonNull_sameValue() {
        initNonNull();
        StatementSpy.checkAtLeastOneUpdate(() -> inTransaction(transaction, () -> {
            delegate.updateOneLazyProperty(em, entityId, "initial2");
        }));
        inTransaction(transaction, () -> {
            delegate.testLazyLoadingAndPersistedValues(em, entityId, "initial1", "initial2", "initial3");
        });
    }

    @Test
    public void update_all_nonNullToNull() {
        initNonNull();
        StatementSpy.checkAtLeastOneUpdate(() -> inTransaction(transaction, () -> {
            delegate.updateAllProperties(em, entityId, null, null, null);
        }));
        inTransaction(transaction, () -> {
            delegate.testLazyLoadingAndPersistedValues(em, entityId, null, null, null);
        });
    }

    @Test
    public void update_allLazy_nonNullToNull() {
        initNonNull();
        StatementSpy.checkAtLeastOneUpdate(() -> inTransaction(transaction, () -> {
            delegate.updateAllLazyProperties(em, entityId, null, null);
        }));
        inTransaction(transaction, () -> {
            delegate.testLazyLoadingAndPersistedValues(em, entityId, "initial1", null, null);
        });
    }

    @Test
    public void update_oneEager_nonNullToNull() {
        initNonNull();
        StatementSpy.checkAtLeastOneUpdate(() -> inTransaction(transaction, () -> {
            delegate.updateOneEagerProperty(em, entityId, null);
        }));
        inTransaction(transaction, () -> {
            delegate.testLazyLoadingAndPersistedValues(em, entityId, null, "initial2", "initial3");
        });
    }

    @Test
    public void update_oneLazy_nonNullToNull() {
        initNonNull();
        StatementSpy.checkAtLeastOneUpdate(() -> inTransaction(transaction, () -> {
            delegate.updateOneLazyProperty(em, entityId, null);
        }));
        inTransaction(transaction, () -> {
            delegate.testLazyLoadingAndPersistedValues(em, entityId, "initial1", null, "initial3");
        });
    }

    private void initNull() {
        inTransaction(transaction, () -> {
            entityId = delegate.create(em, null, null, null);
        });
        inTransaction(transaction, () -> {
            delegate.testLazyLoadingAndPersistedValues(em, entityId, null, null, null);
        });
    }

    private void initNonNull() {
        inTransaction(transaction, () -> {
            entityId = delegate.create(em, "initial1", "initial2", "initial3");
        });
        inTransaction(transaction, () -> {
            delegate.testLazyLoadingAndPersistedValues(em, entityId, "initial1", "initial2", "initial3");
        });
    }

    /**
     * An interface for delegate classes, classes whose bytecode is transformed by Quarkus to replace public field
     * access with getter/setter access.
     * <p>
     * (Test bytecode was not transformed by Quarkus when using QuarkusUnitTest last time I checked).
     */
    interface AccessDelegate {

        long create(EntityManager entityManager, String eagerProperty1, String lazyProperty1, String lazyProperty2);

        void updateAllProperties(EntityManager entityManager, long entityId, String eagerProperty1,
                String lazyProperty1, String lazyProperty2);

        void updateAllLazyProperties(EntityManager entityManager, long entityId, String lazyProperty1,
                String lazyProperty2);

        void updateOneEagerProperty(EntityManager entityManager, long entityId, String eagerProperty1);

        void updateOneLazyProperty(EntityManager entityManager, long entityId, String lazyProperty1);

        void testLazyLoadingAndPersistedValues(EntityManager entityManager, long entityId,
                String expectedEagerProperty1, String expectedLazyProperty1, String expectedLazyProperty2);
    }

    @PersistenceUnitExtension
    public static class StatementSpy implements StatementInspector {
        private static final ThreadLocal<List<String>> statements = new ThreadLocal<>();

        public static void checkAtLeastOneUpdate(Runnable runnable) {
            check(runnable, list -> assertThat(list).isNotEmpty() // Something is wrong if we didn't even load an entity
                    .anySatisfy(sql -> assertThat(sql).containsIgnoringCase("update")));
        }

        public static void checkNoUpdate(Runnable runnable) {
            check(runnable, list -> assertThat(list).isNotEmpty() // Something is wrong if we didn't even load an entity
                    .allSatisfy(sql -> assertThat(sql).doesNotContainIgnoringCase("update")));
        }

        public static void check(Runnable runnable, Consumer<List<String>> assertion) {
            List<String> list = new ArrayList<>();
            if (statements.get() != null) {
                throw new IllegalStateException("Cannot nest checkNoUpdate()");
            }
            statements.set(list);
            runnable.run();
            statements.remove();
            assertion.accept(list);
        }

        @Override
        public String inspect(String sql) {
            List<String> list = statements.get();
            if (list != null) {
                list.add(sql);
            }
            return sql;
        }
    }
}
