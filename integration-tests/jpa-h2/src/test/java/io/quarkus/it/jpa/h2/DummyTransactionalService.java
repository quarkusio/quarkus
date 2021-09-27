package io.quarkus.it.jpa.h2;

import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;

import io.quarkus.narayana.jta.runtime.TransactionConfiguration;
import io.quarkus.narayana.jta.runtime.interceptor.RunnableWithException;
import io.quarkus.test.junit.QuarkusTest;

@ApplicationScoped
@QuarkusTest
public class DummyTransactionalService {
    @Transactional
    public void doWithinPlainTransactional(RunnableWithException todo) throws Exception {
        todo.run();
    }

    @Transactional
    @TransactionConfiguration(timeoutFromConfigProperty = "transaction.timeout.unknown")
    public void doWithinTransactionWithMissingProperty(RunnableWithException todo) throws Exception {
        todo.run();
    }

    @Transactional
    @TransactionConfiguration(timeoutFromConfigProperty = "transaction.timeout.unknown", timeout = 2)
    public void doWithinTransactionWithMissingPropertyAndSetTimeoutInAnnotation(RunnableWithException todo)
            throws Exception {
        todo.run();
    }

    @Transactional
    @TransactionConfiguration(timeoutFromConfigProperty = "transaction.timeout.1s", timeout = 2)
    public void doWithinTransactionWithPropertyAndSetTimeoutInAnnotation(RunnableWithException todo)
            throws Exception {
        todo.run();
    }

    @Transactional
    @TransactionConfiguration(timeoutFromConfigProperty = "transaction.timeout.1s")
    public void doWithinTransactionWithProperty(RunnableWithException todo)
            throws Exception {
        todo.run();
    }

    @Transactional
    @TransactionConfiguration()
    public void doWithinTransactionWithNothing(RunnableWithException todo)
            throws Exception {
        todo.run();
    }
}
