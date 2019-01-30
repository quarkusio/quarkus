package org.infinispan.protean.hibernate.cache;

import org.hibernate.cache.spi.CacheTransactionSynchronization;
import org.hibernate.cache.spi.RegionFactory;
import org.jboss.logging.Logger;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Function;

final class Sync implements CacheTransactionSynchronization {
    private static final Logger log = Logger.getLogger(Sync.class);
    private final static boolean trace = log.isTraceEnabled();

    private final RegionFactory regionFactory;
    private long transactionStartTimestamp;
    // to save some allocations we're storing everything in a single array
    private Object[] tasks;
    private int index;

    Sync(RegionFactory regionFactory) {
        this.regionFactory = regionFactory;
        transactionStartTimestamp = regionFactory.nextTimestamp();
    }

    public void registerBeforeCommit(CompletableFuture<?> future) {
        add(future);
    }

    public void registerAfterCommit(Function<Boolean, CompletableFuture<?>> invocation) {
        assert !(invocation instanceof CompletableFuture) : "Invocation must not extend CompletableFuture";
        add(invocation);
    }

    private void add(Object task) {
        log.tracef("Adding %08x %s", System.identityHashCode(task), task);
        if (tasks == null) {
            tasks = new Object[4];
        } else if (index == tasks.length) {
            tasks = Arrays.copyOf(tasks, tasks.length * 2);
        }
        tasks[index++] = task;
    }

    public long getCurrentTransactionStartTimestamp() {
        return transactionStartTimestamp;
    }

    @Override
    public void transactionJoined() {
        transactionStartTimestamp = regionFactory.nextTimestamp();
    }

    @Override
    public void transactionCompleting() {
        if (trace) {
            int done = 0, notDone = 0;
            for (int i = 0; i < index; ++i) {
                Object task = tasks[i];
                if (task instanceof CompletableFuture) {
                    if (((CompletableFuture) task).isDone()) {
                        done++;
                    } else {
                        notDone++;
                    }
                }
            }
            log.tracef("%d tasks done, %d tasks not done yet", done, notDone);
        }
        int count = 0;
        for (int i = 0; i < index; ++i) {
            Object task = tasks[i];
            if (task instanceof CompletableFuture) {
                log.tracef("Waiting for %08x %s", System.identityHashCode(task), task);
                try {
                    ((CompletableFuture) task).join();
                } catch (CompletionException e) {
                    log.errorf(e, "Operation #%d scheduled to complete before transaction completion failed", i);
                }
                tasks[i] = null;
                ++count;
            } else {
                log.tracef("Not waiting for %08x %s", System.identityHashCode(task), task);
            }
        }
        if (trace) {
            log.tracef("Finished %d tasks before completion", count);
        }
    }

    @Override
    public void transactionCompleted(boolean successful) {
        if (!successful) {
            // When the transaction is rolled back transactionCompleting() is not called,
            // so we could have some completable futures waiting.
            transactionCompleting();
        }
        int invoked = 0, waiting = 0;
        for (int i = 0; i < index; ++i) {
            Object invocation = tasks[i];
            if (invocation == null) {
                continue;
            }
            try {
                tasks[i] = ((Function<Boolean, CompletableFuture<?>>) invocation).apply(successful);
            } catch (Exception e) {
                log.errorf(e, "Operation #%d scheduled after transaction completion failed (transaction successful? %s)", i, successful);
                tasks[i] = null;
            }
            invoked++;
        }
        for (int i = 0; i < index; ++i) {
            CompletableFuture<?> cf = (CompletableFuture<?>) tasks[i];
            if (cf != null) {
                try {
                    cf.join();
                } catch (Exception e) {
                    log.errorf(e, "Operation #%d scheduled after transaction completion failed (transaction successful? %s)", i, successful);
                }
                waiting++;
            }
        }
        if (trace) {
            log.tracef("Invoked %d tasks after completion, %d are synchronous.", invoked, waiting);
        }
    }
}
