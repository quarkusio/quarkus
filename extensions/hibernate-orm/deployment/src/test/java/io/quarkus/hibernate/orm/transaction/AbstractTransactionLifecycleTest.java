package io.quarkus.hibernate.orm.transaction;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.logging.Level;

import javax.persistence.EntityManager;

import org.hibernate.BaseSessionEventListener;
import org.hibernate.Session;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

/**
 * Check transaction lifecycle, including session flushes and the closing of the session.
 */
public abstract class AbstractTransactionLifecycleTest {

    private static final String INITIAL_NAME = "Initial name";
    private static final String UPDATED_NAME = "Updated name";

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(SimpleEntity.class)
                    .addAsResource("application.properties"))
            // Expect no warnings (in particular from Agroal)
            .setLogRecordPredicate(record -> record.getLevel().intValue() >= Level.WARNING.intValue())
            .assertLogRecords(records -> assertThat(records).isEmpty());

    @Test
    public void testLifecycle() {
        long id = 1L;
        TestCRUD crud = crud();

        ValueAndExecutionMetadata<Void> created = crud.create(id, INITIAL_NAME);
        checkPostConditions(created,
                LifecycleOperation.FLUSH, LifecycleOperation.STATEMENT, // update
                expectDoubleFlush() ? LifecycleOperation.FLUSH : null,
                LifecycleOperation.TRANSACTION_COMPLETION);

        ValueAndExecutionMetadata<String> retrieved = crud.retrieve(id);
        checkPostConditions(retrieved,
                LifecycleOperation.STATEMENT, // select
                LifecycleOperation.FLUSH,
                expectDoubleFlush() ? LifecycleOperation.FLUSH : null,
                LifecycleOperation.TRANSACTION_COMPLETION);
        assertThat(retrieved.value).isEqualTo(INITIAL_NAME);

        ValueAndExecutionMetadata<Void> updated = crud.update(id, UPDATED_NAME);
        checkPostConditions(updated,
                LifecycleOperation.STATEMENT, // select
                LifecycleOperation.FLUSH, LifecycleOperation.STATEMENT, // update
                expectDoubleFlush() ? LifecycleOperation.FLUSH : null,
                LifecycleOperation.TRANSACTION_COMPLETION);

        retrieved = crud.retrieve(id);
        checkPostConditions(retrieved,
                LifecycleOperation.STATEMENT, // select
                LifecycleOperation.FLUSH,
                expectDoubleFlush() ? LifecycleOperation.FLUSH : null,
                LifecycleOperation.TRANSACTION_COMPLETION);
        assertThat(retrieved.value).isEqualTo(UPDATED_NAME);

        ValueAndExecutionMetadata<Void> deleted = crud.delete(id);
        checkPostConditions(deleted,
                LifecycleOperation.STATEMENT, // select
                LifecycleOperation.FLUSH, LifecycleOperation.STATEMENT, // delete
                // No double flush here, since there's nothing in the session after the first flush.
                LifecycleOperation.TRANSACTION_COMPLETION);

        retrieved = crud.retrieve(id);
        checkPostConditions(retrieved,
                LifecycleOperation.STATEMENT, // select
                LifecycleOperation.TRANSACTION_COMPLETION);
        assertThat(retrieved.value).isNull();
    }

    protected abstract TestCRUD crud();

    protected abstract boolean expectDoubleFlush();

    private void checkPostConditions(ValueAndExecutionMetadata<?> result, LifecycleOperation... expectedOperationsArray) {
        List<LifecycleOperation> expectedOperations = new ArrayList<>();
        Collections.addAll(expectedOperations, expectedOperationsArray);
        expectedOperations.removeIf(Objects::isNull);
        // No extra statements or flushes
        assertThat(result.listener.operations)
                .containsExactlyElementsOf(expectedOperations);
        // Session was closed automatically
        assertThat(result.sessionImplementor).returns(true, SharedSessionContractImplementor::isClosed);
    }

    public abstract static class TestCRUD {
        public ValueAndExecutionMetadata<Void> create(long id, String name) {
            return inTransaction(entityManager -> {
                SimpleEntity entity = new SimpleEntity(name);
                entity.setId(id);
                entityManager.persist(entity);
                return null;
            });
        }

        public ValueAndExecutionMetadata<String> retrieve(long id) {
            return inTransaction(entityManager -> {
                SimpleEntity entity = entityManager.find(SimpleEntity.class, id);
                return entity == null ? null : entity.getName();
            });
        }

        public ValueAndExecutionMetadata<Void> update(long id, String name) {
            return inTransaction(entityManager -> {
                SimpleEntity entity = entityManager.find(SimpleEntity.class, id);
                entity.setName(name);
                return null;
            });
        }

        public ValueAndExecutionMetadata<Void> delete(long id) {
            return inTransaction(entityManager -> {
                SimpleEntity entity = entityManager.find(SimpleEntity.class, id);
                entityManager.remove(entity);
                return null;
            });
        }

        public abstract <T> ValueAndExecutionMetadata<T> inTransaction(Function<EntityManager, T> action);
    }

    protected static class ValueAndExecutionMetadata<T> {

        public static <T> ValueAndExecutionMetadata<T> run(EntityManager entityManager, Function<EntityManager, T> action) {
            LifecycleListener listener = new LifecycleListener();
            entityManager.unwrap(Session.class).addEventListeners(listener);
            T result = action.apply(entityManager);
            return new ValueAndExecutionMetadata<>(result, entityManager, listener);
        }

        final T value;
        final SessionImplementor sessionImplementor;
        final LifecycleListener listener;

        private ValueAndExecutionMetadata(T value, EntityManager entityManager, LifecycleListener listener) {
            this.value = value;
            // Make sure we don't return a wrapper, but the actual implementation.
            this.sessionImplementor = entityManager.unwrap(SessionImplementor.class);
            this.listener = listener;
        }
    }

    private static class LifecycleListener extends BaseSessionEventListener {
        private final List<LifecycleOperation> operations = new ArrayList<>();

        @Override
        public void jdbcExecuteStatementStart() {
            operations.add(LifecycleOperation.STATEMENT);
        }

        @Override
        public void flushStart() {
            operations.add(LifecycleOperation.FLUSH);
        }

        @Override
        public void transactionCompletion(boolean successful) {
            operations.add(LifecycleOperation.TRANSACTION_COMPLETION);
        }
    }

    private enum LifecycleOperation {
        STATEMENT,
        FLUSH,
        TRANSACTION_COMPLETION;
    }
}
