package io.quarkus.narayana.jta;

import java.util.concurrent.Callable;
import java.util.function.Function;

class TransactionRunnerImpl extends RunOptionsBase implements TransactionRunnerOptions, TransactionRunner {
    TransactionRunnerImpl(TransactionSemantics semantics) {
        setSemantics(semantics);
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
