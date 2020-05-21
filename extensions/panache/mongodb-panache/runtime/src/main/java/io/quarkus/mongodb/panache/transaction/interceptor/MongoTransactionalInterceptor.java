package io.quarkus.mongodb.panache.transaction.interceptor;

import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;

import org.reactivestreams.Publisher;

import com.arjuna.ats.jta.logging.jtaLogger;
import com.mongodb.ReadConcern;
import com.mongodb.ReadConcernLevel;
import com.mongodb.ReadPreference;
import com.mongodb.TransactionOptions;
import com.mongodb.WriteConcern;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;

import io.quarkus.mongodb.panache.transaction.MongoTransactionException;
import io.quarkus.mongodb.panache.transaction.MongoTransactional;
import io.quarkus.narayana.jta.runtime.interceptor.RunnableWithException;
import io.smallrye.mutiny.Multi;
import io.smallrye.reactive.converters.ReactiveTypeConverter;
import io.smallrye.reactive.converters.Registry;

@Interceptor
@MongoTransactional
@Priority(Interceptor.Priority.PLATFORM_BEFORE + 200)
public class MongoTransactionalInterceptor {
    @Inject
    MongoClient client;

    @Inject
    TransactionSynchronizationRegistry tsr;

    @Inject
    TransactionManager transactionManager;

    @AroundInvoke
    public Object intercept(InvocationContext ic) throws Exception {
        // if we are in a sub-method call we just proceed with the invocation
        // note that nested transactions are not supported
        // maybe we need to rely on clientSession.hasActiveTransaction ?
        if (isInTransaction()) {
            return ic.proceed();
        }

        MongoTransactional transactional = getTransactional(ic);
        TransactionOptions.Builder txnOptions = TransactionOptions.builder();
        if (!transactional.readConcern().equals("<default>")) {
            ReadConcern readConcern = new ReadConcern(ReadConcernLevel.fromString(transactional.readConcern()));
            txnOptions.readConcern(readConcern);
        }
        if (!transactional.writeConcern().equals("<default>")) {
            WriteConcern writeConcern = WriteConcern.valueOf(transactional.writeConcern());
            txnOptions.writeConcern(writeConcern);
        }
        if (!transactional.readPreference().equals("<default>")) {
            ReadPreference readPreference = ReadPreference.valueOf(transactional.readPreference());
            txnOptions.readPreference(readPreference);
        }

        transactionManager.begin();
        MongoTransaction mongoTransaction = createMongoTransaction();
        boolean throwing = false;
        Object result = null;

        try {
            ClientSession clientSession = client.startSession();
            clientSession.startTransaction(txnOptions.build());
            mongoTransaction.setClientSession(clientSession);
            result = ic.proceed();
        } catch (Throwable t) {
            throwing = true;
            transactionManager.setRollbackOnly();
            throw t;
        } finally {
            Transaction transaction = transactionManager.getTransaction();
            // handle asynchronously if not throwing
            if (!throwing && result != null) {
                ReactiveTypeConverter<Object> converter = null;
                if (result instanceof CompletionStage == false
                        && (result instanceof Publisher == false || ic.getMethod().getReturnType() != Publisher.class)) {
                    @SuppressWarnings({ "rawtypes", "unchecked" })
                    Optional<ReactiveTypeConverter<Object>> lookup = Registry.lookup((Class) result.getClass());
                    if (lookup.isPresent()) {
                        converter = lookup.get();
                        if (converter.emitAtMostOneItem()) {
                            result = converter.toCompletionStage(result);
                        } else {
                            result = converter.toRSPublisher(result);
                        }
                    }
                }
                if (result instanceof CompletionStage) {
                    result = handleAsync(transactionManager, transaction, result);
                    // convert back
                    if (converter != null)
                        result = converter.fromCompletionStage((CompletionStage<?>) result);
                } else if (result instanceof Publisher) {
                    result = handleAsync(transactionManager, transaction, result);
                    // convert back
                    if (converter != null)
                        result = converter.fromPublisher((Publisher<?>) result);
                } else {
                    // not async: handle synchronously
                    endTransaction(transactionManager, transaction);
                }
            } else {
                // throwing or null: handle synchronously
                endTransaction(transactionManager, transaction);
            }
        }
        return result;
    }

    private Object handleAsync(TransactionManager tm, Transaction tx, Object ret) throws Exception {
        // Suspend the transaction to remove it from the main request thread
        tm.suspend();
        if (ret instanceof CompletionStage) {
            return ((CompletionStage<?>) ret).handle((v, t) -> {
                try {
                    doInTransaction(tm, tx, () -> {
                        endTransaction(tm, tx);
                    });
                } catch (RuntimeException e) {
                    if (t != null)
                        e.addSuppressed(t);
                    throw e;
                } catch (Exception e) {
                    CompletionException x = new CompletionException(e);
                    if (t != null)
                        x.addSuppressed(t);
                    throw x;
                }
                // pass-through the previous results
                if (t instanceof RuntimeException)
                    throw (RuntimeException) t;
                if (t != null)
                    throw new CompletionException(t);
                return v;
            });
        } else if (ret instanceof Publisher) {
            ret = Multi.createFrom().publisher((Publisher<?>) ret)
                    .onFailure().invoke(t -> {
                        try {
                            doInTransaction(tm, tx, () -> {
                            });
                        } catch (RuntimeException e) {
                            e.addSuppressed(t);
                            throw e;
                        } catch (Exception e) {
                            RuntimeException x = new RuntimeException(e);
                            x.addSuppressed(t);
                            throw x;
                        }
                        // pass-through the previous result
                        if (t instanceof RuntimeException)
                            throw (RuntimeException) t;
                        throw new RuntimeException(t);
                    }).on().termination(() -> {
                        try {
                            doInTransaction(tm, tx, () -> endTransaction(tm, tx));
                        } catch (RuntimeException e) {
                            throw e;
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    });
        }
        return ret;
    }

    private void doInTransaction(TransactionManager tm, Transaction tx, RunnableWithException f) throws Exception {
        // Verify if this thread's transaction is the right one
        Transaction currentTransaction = tm.getTransaction();
        // If not, install the right transaction
        if (currentTransaction != tx) {
            if (currentTransaction != null)
                tm.suspend();
            tm.resume(tx);
        }
        f.run();
        if (currentTransaction != tx) {
            tm.suspend();
            if (currentTransaction != null)
                tm.resume(currentTransaction);
        }
    }

    private void endTransaction(TransactionManager tm, Transaction tx)
            throws Exception {
        if (tx != tm.getTransaction()) {
            throw new RuntimeException(jtaLogger.i18NLogger.get_wrong_tx_on_thread());
        }

        if (tm.getStatus() == Status.STATUS_MARKED_ROLLBACK) {
            tm.rollback();
        } else {
            tm.commit();
        }
    }

    private MongoTransaction createMongoTransaction() {
        // already have a transaction
        final MongoTransaction existing = (MongoTransaction) tsr.getResource(tsr.getTransactionKey());
        if (existing != null) {
            return existing;
        }

        // creates a new one
        final MongoTransaction transaction = new MongoTransaction();
        tsr.putResource(tsr.getTransactionKey(), transaction);
        tsr.registerInterposedSynchronization(new Synchronization() {
            @Override
            public void beforeCompletion() {
            }

            @Override
            public void afterCompletion(int i) {
                try {
                    if (transactionManager.getStatus() == Status.STATUS_ROLLEDBACK) {
                        transaction.rollback();
                    } else {
                        transaction.commit();
                    }
                } catch (SystemException | RollbackException | HeuristicMixedException | HeuristicRollbackException e) {
                    throw new MongoTransactionException(e);
                }
            }
        });
        return transaction;
    }

    private boolean isInTransaction() {
        try {
            switch (transactionManager.getStatus()) {
                case Status.STATUS_ACTIVE:
                case Status.STATUS_COMMITTING:
                case Status.STATUS_MARKED_ROLLBACK:
                case Status.STATUS_PREPARED:
                case Status.STATUS_PREPARING:
                    return true;
                default:
                    return false;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private MongoTransactional getTransactional(InvocationContext ic) {
        MongoTransactional configuration = ic.getMethod().getAnnotation(MongoTransactional.class);
        if (configuration == null) {
            return ic.getTarget().getClass().getAnnotation(MongoTransactional.class);
        }

        return configuration;
    }
}
