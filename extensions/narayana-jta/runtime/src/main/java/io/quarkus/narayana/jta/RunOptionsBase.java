package io.quarkus.narayana.jta;

import java.util.function.Function;

/**
 * An abstract base for both {@link RunOptions} and {@link TransactionRunnerImpl}.
 * <p>
 * Necessary because having {@link RunOptions} extend {@link TransactionRunnerImpl}., or the other way around, results
 * in signature conflicts in {@code exceptionHandler(Function)}.
 */
class RunOptionsBase {
    TransactionSemantics semantics = TransactionSemantics.REQUIRE_NEW;
    int timeout = 0;
    Function<Throwable, TransactionExceptionResult> exceptionHandler;

    RunOptionsBase setTimeout(int seconds) {
        if (seconds < 0) {
            throw new IllegalArgumentException("seconds cannot be negative");
        }
        this.timeout = seconds;
        return this;
    }

    RunOptionsBase setSemantics(TransactionSemantics semantics) {
        this.semantics = semantics;
        return this;
    }

    RunOptionsBase setExceptionHandler(Function<Throwable, TransactionExceptionResult> handler) {
        this.exceptionHandler = handler;
        return this;
    }
}
