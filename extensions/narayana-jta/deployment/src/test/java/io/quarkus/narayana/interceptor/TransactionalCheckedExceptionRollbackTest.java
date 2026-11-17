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
 * The default behavior since Quarkus 4: a checked exception crossing the boundary of a {@code @Transactional}
 * method rolls back the transaction, which deviates from the Jakarta Transactions specification.
 */
public class TransactionalCheckedExceptionRollbackTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(TransactionalCheckedExceptionBean.class, TestXAResource.class, TxAssertionData.class,
                            TestException.class, AnnotatedTestException.class))
            .setLogRecordPredicate(record -> record.getLevel().intValue() >= Level.WARNING.intValue())
            // the default behavior is not something the application needs to be warned about
            .assertLogRecords(records -> assertThat(records)
                    .noneMatch(record -> record.getMessage().contains("was not marked for rollback")));

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
    public void checkedExceptionRollsBack() {
        assertThatThrownBy(() -> bean.throwCheckedException()).isInstanceOf(TestException.class);

        assertThat(txAssertionData.getCommit()).isZero();
        assertThat(txAssertionData.getRollback()).isOne();
    }

    @Test
    public void annotatedCheckedExceptionRollsBack() {
        assertThatThrownBy(() -> bean.throwAnnotatedCheckedException()).isInstanceOf(AnnotatedTestException.class);

        assertThat(txAssertionData.getCommit()).isZero();
        assertThat(txAssertionData.getRollback()).isOne();
    }

    @Test
    public void dontRollbackOnStillWins() {
        assertThatThrownBy(() -> bean.throwCheckedExceptionDontRollbackOn()).isInstanceOf(TestException.class);

        assertThat(txAssertionData.getCommit()).isOne();
        assertThat(txAssertionData.getRollback()).isZero();
    }

    @Test
    public void runtimeExceptionRollsBack() {
        assertThatThrownBy(() -> bean.throwRuntimeException()).isInstanceOf(IllegalStateException.class);

        assertThat(txAssertionData.getCommit()).isZero();
        assertThat(txAssertionData.getRollback()).isOne();
    }

    @Test
    public void successCommits() throws Exception {
        bean.succeed();

        assertThat(txAssertionData.getCommit()).isOne();
        assertThat(txAssertionData.getRollback()).isZero();
    }
}
