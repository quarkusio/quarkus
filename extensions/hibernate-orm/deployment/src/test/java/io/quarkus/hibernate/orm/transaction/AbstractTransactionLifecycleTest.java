package io.quarkus.hibernate.orm.transaction;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import javax.persistence.EntityManager;
import javax.persistence.ParameterMode;
import javax.persistence.StoredProcedureQuery;

import org.hibernate.BaseSessionEventListener;
import org.hibernate.Session;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.agroal.api.AgroalDataSource;
import io.quarkus.arc.Arc;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Check transaction lifecycle, including session flushes, the closing of the session,
 * and the release of JDBC resources.
 */
public abstract class AbstractTransactionLifecycleTest {

    private static final String INITIAL_NAME = "Initial name";
    private static final String UPDATED_NAME = "Updated name";

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(SimpleEntity.class)
                    .addAsResource("application.properties"))
            // Expect no warnings (in particular from Agroal)
            .setLogRecordPredicate(record -> record.getLevel().intValue() >= Level.WARNING.intValue()
                    // Ignore these particular warnings: they are not relevant to this test.
                    && !record.getMessage().contains("has been blocked for") //sometimes CI has a super slow moment and this triggers the blocked thread detector
                    && !record.getMessage().contains("Using Java versions older than 11 to build Quarkus applications")
                    && !record.getMessage().contains("Agroal does not support detecting if a connection is still usable")
                    && !record.getMessage().contains("Netty DefaultChannelId initialization"))
            .assertLogRecords(records -> assertThat(records)
                    .extracting(LogRecord::getMessage) // This is just to get meaningful error messages, as LogRecord doesn't have a toString()
                    .isEmpty());

    @BeforeAll
    public static void installStoredProcedure() throws SQLException {
        AgroalDataSource dataSource = Arc.container().instance(AgroalDataSource.class).get();
        try (Connection conn = dataSource.getConnection()) {
            try (Statement st = conn.createStatement()) {
                st.execute("CREATE ALIAS " + MyStoredProcedure.NAME
                        + " FOR \"" + MyStoredProcedure.class.getName() + ".execute\"");
            }
        }
    }

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

        // See https://github.com/quarkusio/quarkus/issues/13273
        ValueAndExecutionMetadata<String> calledStoredProcedure = crud.callStoredProcedure(id);
        checkPostConditions(calledStoredProcedure,
                // Strangely, calling a stored procedure isn't considered as a statement for Hibernate ORM listeners
                LifecycleOperation.TRANSACTION_COMPLETION);
        assertThat(calledStoredProcedure.value).isEqualTo(MyStoredProcedure.execute(id));

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

        public ValueAndExecutionMetadata<String> callStoredProcedure(long id) {
            return inTransaction(entityManager -> {
                StoredProcedureQuery storedProcedure = entityManager.createStoredProcedureQuery(MyStoredProcedure.NAME);
                storedProcedure.registerStoredProcedureParameter(0, Long.class, ParameterMode.IN);
                storedProcedure.setParameter(0, id);
                storedProcedure.execute();
                return (String) storedProcedure.getSingleResult();
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

    public static class MyStoredProcedure {
        private static final String NAME = "myStoredProc";
        private static final String RESULT_PREFIX = "StoredProcResult";

        @SuppressWarnings("unused")
        public static String execute(long id) {
            return RESULT_PREFIX + id;
        }
    }
}
