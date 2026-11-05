package io.quarkus.narayana.interceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.logging.Level;

import jakarta.inject.Inject;
import jakarta.transaction.UserTransaction;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

/**
 * {@code quarkus.transactional.checked-exceptions=no-rollback} property restores the Jakarta Transactions
 * specification behaviour, and warns whenever a checked exception escapes without a rollback.
 *
 * @see TransactionalCheckedExceptionRollbackTest
 */
public class TransactionalCheckedExceptionNoRollbackTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(TransactionalCheckedExceptionBean.class, TestXAResource.class, TxAssertionData.class,
                            TestException.class, AnnotatedTestException.class))
            .overrideRuntimeConfigKey("quarkus.transactional.checked-exceptions", "no-rollback")
            .setLogRecordPredicate(record -> record.getLevel().intValue() >= Level.WARNING.intValue())
            .assertLogRecords(records -> assertThat(records)
                    .anyMatch(record -> record.getMessage().contains("was not marked for rollback")));

    @Inject
    TransactionalCheckedExceptionBean bean;

    @Inject
    TxAssertionData txAssertionData;

    @Inject
    UserTransaction userTransaction;

    @AfterEach
    public void tearDown() {
        try {
            userTransaction.rollback();
        } catch (Exception e) {
            // do nothing
        } finally {
            txAssertionData.reset();
        }
    }

    @Test
    public void checkedExceptionCommits() {
        assertThatThrownBy(() -> bean.throwCheckedException()).isInstanceOf(TestException.class);

        assertThat(txAssertionData.getCommit()).isOne();
        assertThat(txAssertionData.getRollback()).isZero();
    }

    @Test
    public void annotatedCheckedExceptionStillRollsBack() {
        assertThatThrownBy(() -> bean.throwAnnotatedCheckedException()).isInstanceOf(AnnotatedTestException.class);

        assertThat(txAssertionData.getCommit()).isZero();
        assertThat(txAssertionData.getRollback()).isOne();
    }

    @Test
    public void runtimeExceptionStillRollsBack() {
        assertThatThrownBy(() -> bean.throwRuntimeException()).isInstanceOf(IllegalStateException.class);

        assertThat(txAssertionData.getCommit()).isZero();
        assertThat(txAssertionData.getRollback()).isOne();
    }
}
