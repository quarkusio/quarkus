package io.quarkus.narayana.jta;

import java.util.concurrent.Callable;
import java.util.function.Function;

class TransactionRunnerImpl extends RunOptions
        implements TransactionRunnerSemanticOptions, TransactionRunnerRunOptions, TransactionRunner {
    @Override
    public TransactionRunnerImpl joinExisting() {
        return semantic(Semantic.JOIN_EXISTING);
    }

    @Override
    public TransactionRunnerImpl requireNew() {
        return semantic(Semantic.REQUIRE_NEW);
    }

    @Override
    public TransactionRunnerImpl suspendExisting() {
        return semantic(Semantic.SUSPEND_EXISTING);
    }

    @Override
    public TransactionRunnerImpl disallowExisting() {
        return semantic(Semantic.DISALLOW_EXISTING);
    }

    @Override
    public TransactionRunnerImpl semantic(Semantic semantic) {
        super.semantic(semantic);
        return this;
    }

    @Override
    public TransactionRunnerImpl timeout(int seconds) {
        super.timeout(seconds);
        return this;
    }

    @Override
    public TransactionRunnerImpl exceptionHandler(Function<Throwable, ExceptionResult> handler) {
        super.exceptionHandler(handler);
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
