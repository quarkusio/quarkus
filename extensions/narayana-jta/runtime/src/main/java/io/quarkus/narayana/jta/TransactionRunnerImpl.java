package io.quarkus.narayana.jta;

import java.util.concurrent.Callable;
import java.util.function.Function;

class TransactionRunnerImpl extends RunOptionsBase
        implements TransactionRunnerSemanticOptions, TransactionRunnerRunOptions, TransactionRunner {
    @Override
    public TransactionRunnerImpl joinExisting() {
        return semantic(TransactionSemantic.JOIN_EXISTING);
    }

    @Override
    public TransactionRunnerImpl requireNew() {
        return semantic(TransactionSemantic.REQUIRE_NEW);
    }

    @Override
    public TransactionRunnerImpl suspendExisting() {
        return semantic(TransactionSemantic.SUSPEND_EXISTING);
    }

    @Override
    public TransactionRunnerImpl disallowExisting() {
        return semantic(TransactionSemantic.DISALLOW_EXISTING);
    }

    @Override
    public TransactionRunnerImpl semantic(TransactionSemantic semantic) {
        setSemantic(semantic);
        return this;
    }

    @Override
    public TransactionRunnerImpl timeout(int seconds) {
        setTimeout(seconds);
        return this;
    }

    @Override
    public TransactionRunnerImpl exceptionHandler(Function<Throwable, TransactionExceptionResult> handler) {
        setExceptionHandler(handler);
        return this;
    }

    @Override
    public void run(Runnable task) {
        QuarkusTransactionImpl.call(this, () -> {
            task.run();
            return null;
        });
    }

    @Override
    public <T> T call(Callable<T> task) {
        return QuarkusTransactionImpl.call(this, task);
    }
}
