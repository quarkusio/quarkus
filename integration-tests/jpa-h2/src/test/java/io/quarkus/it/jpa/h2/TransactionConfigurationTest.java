package io.quarkus.it.jpa.h2;

import static org.junit.jupiter.api.Assertions.assertThrows;

import javax.inject.Inject;
import javax.transaction.RollbackException;

import org.junit.jupiter.api.Test;

import io.quarkus.narayana.jta.runtime.interceptor.RunnableWithException;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
/**
 * Timeouts:
 *
 */
public class TransactionConfigurationTest {
    @Inject
    DummyTransactionalService dummyService;

    @Test
    public void testMissingTransactionConfigurationPropertyIsIgnoredAndDefaultTimeoutIsUsed() throws Exception {
        dummyService.doWithinTransactionWithMissingProperty(someTaskTakingABitLongerThan1Second());
        dummyService.doWithinTransactionWithMissingProperty(someTaskTakingABitLongerThan2Second());
    }

    @Test
    public void testMissingTransactionConfigurationPropertyIsIgnoredAndAnnotationsTimeoutIsUsed() throws Exception {
        dummyService.doWithinTransactionWithMissingPropertyAndSetTimeoutInAnnotation(someTaskTakingABitLongerThan1Second());
        assertThrows(RollbackException.class, () -> dummyService
                .doWithinTransactionWithMissingPropertyAndSetTimeoutInAnnotation(someTaskTakingABitLongerThan2Second()));
    }

    @Test
    public void testTransactionConfigurationPropertyOverridesAnnotationsTimeout() {
        assertThrows(RollbackException.class, () -> dummyService
                .doWithinTransactionWithPropertyAndSetTimeoutInAnnotation(someTaskTakingABitLongerThan1Second()));
    }

    @Test
    public void testTransactionConfigurationPropertyOverridesDefaultTimeout() {
        assertThrows(RollbackException.class, () -> dummyService
                .doWithinTransactionWithProperty(someTaskTakingABitLongerThan1Second()));
    }

    @Test
    public void testTransactionConfigurationCheckFailsIfTransactionConfigurationIsNotUsedAtTransactionEntryLevel() {
        assertThrows(RuntimeException.class, () -> dummyService
                .doWithinPlainTransactional(() -> dummyService.doWithinTransactionWithProperty(() -> {
                })));
    }

    @Test
    public void testTransactionConfigurationCheckSucceedsIfTransactionConfigurationIsNotUsedAtTransactionEntryLevelButHasNoValuesSet()
            throws Exception {
        dummyService
                .doWithinPlainTransactional(() -> dummyService.doWithinTransactionWithNothing(() -> {
                }));
    }

    private RunnableWithException someTaskTakingABitLongerThan1Second() {
        return () -> Thread.sleep(1100);
    }

    private RunnableWithException someTaskTakingABitLongerThan2Second() {
        return () -> Thread.sleep(2100);
    }
}
