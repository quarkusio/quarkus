package io.quarkus.narayana.jta;

import java.util.concurrent.Callable;
import java.util.function.Function;

import jakarta.transaction.Status;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transactional;

import com.arjuna.ats.jta.UserTransaction;

/**
 * A simplified transaction interface. While broadly covering the same use cases as {@link jakarta.transaction.UserTransaction},
 * this class is designed to be easier to use. The main features it offers over {@code UserTransaction} are:
 *
 * <ul>
 * <li><b>No Checked Exceptions: </b>All underlying checked exceptions are wrapped in an unchecked
 * {@link QuarkusTransactionException}.</li>
 * <li><b>No Transaction Leaks: </b>Transactions are tied to the request scope, if the scope is destroyed before the transaction
 * is committed the transaction is rolled back. Note that this means this can only currently be used when the request scope is
 * active.</li>
 * <li><b>Per Transaction Timeouts:</b>
 * {{@link BeginOptions#timeout(int)}/{@link TransactionRunnerOptions#timeout(int)}
 * can be used to set the new transactions timeout, without affecting the per thread default.</li>
 * <li><b>Lambda Style Transactions: </b> {@link Runnable} and {@link Callable} instances can be run inside the scope of a new
 * transaction.</li>
 * </ul>
 * <p>
 * Note that any checked exception will be wrapped by a {@link QuarkusTransactionException}, while unchecked exceptions are
 * allowed to propagate unchanged.
 */
public interface QuarkusTransaction {

    /**
     * Starts a transaction, using the system default timeout.
     * <p>
     * This transaction will be tied to the current request scope, if it is not committed when the scope is destroyed then it
     * will be rolled back to prevent transaction leaks.
     */
    static void begin() {
        begin(beginOptions());
    }

    /**
     * Starts a transaction, using the system default timeout.
     * <p>
     * This transaction will be tied to the current request scope, if it is not committed when the scope is destroyed then it
     * will be rolled back to prevent transaction leaks.
     *
     * @param options Options that apply to the new transaction
     */
    static void begin(BeginOptions options) {
        QuarkusTransactionImpl.begin(options);
    }

    /**
     * Commits the current transaction.
     */
    static void commit() {
        QuarkusTransactionImpl.commit();
    }

    /**
     * Rolls back the current transaction.
     */
    static void rollback() {
        QuarkusTransactionImpl.rollback();
    }

    /**
     * Returns whether a transaction is associated to the current request scope. The name of this method is misleading. Don't
     * use it.
     *
     * @return {@code true} if a transaction is associated to the current request scope.
     *
     * @deprecated the name of this method was misleading and it shouldn't be used anymore. Use {@link #getStatus()} for a
     *             better semantic instead.
     */
    @Deprecated(forRemoval = true, since = "3.22")
    static boolean isActive() {
        return getStatus() != Status.STATUS_NO_TRANSACTION;
    }

    /**
     * Returns the status of the current transaction.
     *
     * @return The status of the current transaction based on the {@link Status} constants.
     */
    static int getStatus() {
        try {
            return UserTransaction.userTransaction().getStatus();
        } catch (SystemException e) {
            throw new QuarkusTransactionException(e);
        }
    }

    /**
     * If the transaction is rollback only
     *
     * @return If the transaction has been marked for rollback
     */
    static boolean isRollbackOnly() {
        return getStatus() == Status.STATUS_MARKED_ROLLBACK;
    }

    /**
     * Marks the transaction as rollback only. Operations can still be carried out, however the transaction cannot be
     * successfully committed.
     */
    static void setRollbackOnly() {
        QuarkusTransactionImpl.setRollbackOnly();
    }

    /**
     * Starts the definition of a transaction runner,
     * which can then be used to run a task ({@link Runnable}, {@link Callable}, ...),
     * with {@link TransactionSemantics#JOIN_EXISTING} semantics:
     * <ul>
     * <li>If no transaction is active then a new transaction will be started, and committed when the method ends.
     * <li>If an exception is thrown the exception handler registered by
     * {@link TransactionRunnerOptions#exceptionHandler(Function)} will be called to
     * decide if the TX should be committed or rolled back.
     * <li>If an existing transaction is active then the method is run in the context of the existing transaction. If an
     * exception is thrown the exception handler will be called, however
     * a result of {@link TransactionExceptionResult#ROLLBACK} will result in the TX marked as rollback only, while a result of
     * {@link TransactionExceptionResult#COMMIT} will result in no action being taken.
     * </ul>
     * <p>
     * Examples of use:
     *
     * <pre>{@code
     * QuarkusTransaction.joiningExisting().run(() -> ...);
     * int value = QuarkusTransaction.joiningExisting().call(() -> { ...; return 42; });
     * }</pre>
     *
     * @return An interface that allow various options of a transaction runner to be customized,
     *         or a {@link Runnable}/{@link Callable} to be executed.
     * @see TransactionRunnerOptions
     */
    static TransactionRunnerOptions joiningExisting() {
        return runner(TransactionSemantics.JOIN_EXISTING);
    }

    /**
     * Starts the definition of a transaction runner,
     * which can then be used to run a task ({@link Runnable}, {@link Callable}, ...),
     * with {@link TransactionSemantics#REQUIRE_NEW} semantics:
     * <ul>
     * <li>If an existing transaction is already associated with the current thread then the transaction is suspended,
     * then a new transaction is started which follows all the normal lifecycle rules,
     * and when it's complete the original transaction is resumed.
     * <li>Otherwise a new transaction is started, and follows all the normal lifecycle rules.
     * </ul>
     * <p>
     * Examples of use:
     *
     * <pre>{@code
     * QuarkusTransaction.requiringNew().run(() -> ...);
     * int value = QuarkusTransaction.requiringNew().call(() -> { ...; return 42; });
     * }</pre>
     *
     * @return An interface that allow various options of a transaction runner to be customized,
     *         or a {@link Runnable}/{@link Callable} to be executed.
     * @see TransactionRunnerOptions
     */
    static TransactionRunnerOptions requiringNew() {
        return runner(TransactionSemantics.REQUIRE_NEW);
    }

    /**
     * Starts the definition of a transaction runner,
     * which can then be used to run a task ({@link Runnable}, {@link Callable}, ...),
     * with {@link TransactionSemantics#DISALLOW_EXISTING} semantics:
     * <ul>
     * <li>If a transaction is already associated with the current thread a {@link QuarkusTransactionException} will be thrown,
     * <li>Otherwise a new transaction is started, and follows all the normal lifecycle rules.
     * </ul>
     * <p>
     * Examples of use:
     *
     * <pre>{@code
     * QuarkusTransaction.requiringNew().run(() -> ...);
     * int value = QuarkusTransaction.requiringNew().call(() -> { ...; return 42; });
     * }</pre>
     *
     * @return An interface that allow various options of a transaction runner to be customized,
     *         or a {@link Runnable}/{@link Callable} to be executed.
     * @see TransactionRunnerOptions
     */
    static TransactionRunnerOptions disallowingExisting() {
        return runner(TransactionSemantics.DISALLOW_EXISTING);
    }

    /**
     * Starts the definition of a transaction runner,
     * which can then be used to run a task ({@link Runnable}, {@link Callable}, ...),
     * with {@link TransactionSemantics#SUSPEND_EXISTING} semantics:
     * <ul>
     * <li>If no transaction is active then these semantics are basically a no-op.
     * <li>If a transaction is active then it is suspended, and resumed after the task is run.
     * <li>The exception handler will never be consulted when these semantics are in use, specifying both an exception handler
     * and
     * these semantics are considered an error.
     * <li>These semantics allows for code to easily be run outside the scope of a transaction.
     * </ul>
     * <p>
     * Examples of use:
     *
     * <pre>{@code
     * QuarkusTransaction.requiringNew().run(() -> ...);
     * int value = QuarkusTransaction.requiringNew().call(() -> { ...; return 42; });
     * }</pre>
     *
     * @return An interface that allow various options of a transaction runner to be customized,
     *         or a {@link Runnable}/{@link Callable} to be executed.
     * @see TransactionRunnerOptions
     */
    static TransactionRunnerOptions suspendingExisting() {
        return runner(TransactionSemantics.SUSPEND_EXISTING);
    }

    /**
     * Starts the definition of a transaction runner,
     * which can then be used to run a task ({@link Runnable}, {@link Callable}, ...),
     * following the selected {@link TransactionSemantics}.
     * <p>
     * Examples of use:
     *
     * <pre>{@code
     * QuarkusTransaction.runner(TransactionSemantics.REQUIRE_NEW).run(() -> ...);
     * QuarkusTransaction.runner(TransactionSemantics.JOIN_EXISTING).run(() -> ...);
     * QuarkusTransaction.runner(TransactionSemantics.SUSPEND_EXISTING).run(() -> ...);
     * QuarkusTransaction.runner(TransactionSemantics.DISALLOW_EXISTING).run(() -> ...);
     * int value = QuarkusTransaction.runner(TransactionSemantics.REQUIRE_NEW).call(() -> { ...; return 42; });
     * int value = QuarkusTransaction.runner(TransactionSemantics.JOIN_EXISTING).call(() -> { ...; return 42; });
     * int value = QuarkusTransaction.runner(TransactionSemantics.SUSPEND_EXISTING).call(() -> { ...; return 42; });
     * int value = QuarkusTransaction.runner(TransactionSemantics.DISALLOW_EXISTING).call(() -> { ...; return 42; });
     * }</pre>
     *
     * @param semantics The selected {@link TransactionSemantics}.
     * @return An interface that allow various options of a transaction runner to be customized,
     *         or a {@link Runnable}/{@link Callable} to be executed.
     * @see TransactionRunnerOptions
     */
    static TransactionRunnerOptions runner(TransactionSemantics semantics) {
        return new TransactionRunnerImpl(semantics);
    }

    /**
     * Runs a task in a new transaction with the default timeout. This defaults to {@link Transactional.TxType#REQUIRES_NEW}
     * semantics, however alternate semantics can be requested using {@link #run(RunOptions, Runnable)}.
     *
     * @param task The task to run in a transaction
     * @deprecated For the same semantics, use {@link #requiringNew() <code>QuarkusTransaction.requiringNew().run(task)</code>}.
     *             {@link #joiningExisting()}, {@link #disallowingExisting()}, {@link #suspendingExisting()}
     *             and {@link #runner(TransactionSemantics)} can also be used for alternate semantics and options.
     */
    @Deprecated
    static void run(Runnable task) {
        run(runOptions(), task);
    }

    /**
     * Runs a task in a new transaction with the default timeout. This defaults to {@link Transactional.TxType#REQUIRES_NEW}
     * semantics, however alternate semantics can be specified using the {@code options} parameter.
     *
     * @param options Options that apply to the new transaction
     * @param task The task to run in a transaction
     * @deprecated Use {@link #requiringNew()}, {@link #joiningExisting()}, {@link #disallowingExisting()},
     *             {@link #suspendingExisting()}
     *             or {@link #runner(TransactionSemantics)} instead.
     */
    @Deprecated
    static void run(RunOptions options, Runnable task) {
        call(options, new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                task.run();
                return null;
            }
        });
    }

    /**
     * Calls a task in a new transaction with the default timeout. This defaults to {@link Transactional.TxType#REQUIRES_NEW}
     * semantics, however alternate semantics can be requested using {@link #call(RunOptions, Callable)}.
     * <p>
     * If the task throws a checked exception it will be wrapped with a {@link QuarkusTransactionException}
     *
     * @param task The task to run in a transaction
     * @deprecated For the same semantics, use {@link #requiringNew()
     *             <code>QuarkusTransaction.requiringNew().call(task)</code>}.
     *             {@link #joiningExisting()}, {@link #disallowingExisting()}, {@link #suspendingExisting()}
     *             and {@link #runner(TransactionSemantics)} can also be used for alternate semantics and options.
     */
    @Deprecated
    static <T> T call(Callable<T> task) {
        return call(runOptions(), task);
    }

    /**
     * Calls a task in a new transaction with the default timeout. This defaults to {@link Transactional.TxType#REQUIRES_NEW}
     * semantics, however alternate semantics can be requested using the {@code options} parameter.
     * <p>
     * If the task throws a checked exception it will be wrapped with a {@link QuarkusTransactionException}
     *
     * @param options Options that apply to the new transaction
     * @param task The task to run in a transaction
     * @deprecated Use {@link #requiringNew()}, {@link #joiningExisting()}, {@link #disallowingExisting()},
     *             {@link #suspendingExisting()}
     *             or {@link #runner(TransactionSemantics)} instead.
     */
    @Deprecated
    static <T> T call(RunOptions options, Callable<T> task) {
        return QuarkusTransactionImpl.call(options, task);
    }

    /**
     * @return a new RunOptions
     * @deprecated Use {@link #requiringNew()}, {@link #joiningExisting()}, {@link #disallowingExisting()},
     *             {@link #suspendingExisting()}
     *             or {@link #runner(TransactionSemantics)} instead.
     */
    @Deprecated
    static RunOptions runOptions() {
        return new RunOptions();
    }

    /**
     * @return a new BeginOptions
     */
    static BeginOptions beginOptions() {
        return new BeginOptions();
    }
}
