package io.quarkus.narayana.jta.runtime.internal.tsr;

import java.util.ArrayList;
import java.util.List;

import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;
import jakarta.transaction.TransactionSynchronizationRegistry;

import org.jboss.logging.Logger;

public class AgroalOrderedLastSynchronizationList implements Synchronization {
    private static final Logger LOGGER = Logger.getLogger(AgroalOrderedLastSynchronizationList.class);
    private static final String ADD_SYNC_ERROR = "Syncs are not allowed because the group of synchronizations to which this sync belongs has already ran";
    private static final String REGISTER_SYNC_ERROR = "Syncs are not allowed to be registered when the transaction is in state ";

    // order the groups of synchronization as follows (Agroal is last since it needs to validate that
    // connection wrappers are closed at the right time):
    private static final String[] PKG_PREFIXES = { "", "org.hibernate", "io.agroal.narayana" };

    private final List<SynchronizationGroup> synchGroups = new ArrayList<>();
    private SynchronizationGroup otherSynchs;
    private final TransactionSynchronizationRegistry tsr;
    private volatile Throwable deferredThrowable; // remember the first beforeCompletion exception

    /*
     * Keep track of whether a synchronization group has been processed.
     * If a group of synchs has already been processed then do not allow further synchs to be registered in that group.
     * If a group of synchs is currently being processed then allow it to be registered.
     * But note that no synchronizations can be registered after the transaction has finished preparing.
     */
    private enum ExecutionStatus {
        PENDING, // the synchronization has not started executing
        RUNNING, // the synchronization is executing
        FINISHED // the synchronization has executed
    }

    /*
     * Synchronizations are grouped by package prefix and these groups are ordered such that the
     * synchronizations in the first group execute first, then the second group is processed, etc.
     * In particular, the Agroal synchronization group runs last.
     *
     * The beforeCompletion methods within a group are called in the order they were added,
     * and the afterCompletion methods are ran in the reverse order
     */
    private class SynchronizationGroup implements Synchronization {
        String packagePrefix; // Synchronizations with this package prefix belong to this group
        final List<Synchronization> synchs; // the Synchronizations in the group
        volatile ExecutionStatus status; // track the status to decide when it's too late to allow more registrations

        public SynchronizationGroup(String packagePrefix) {
            this.packagePrefix = packagePrefix;
            this.synchs = new ArrayList<>();
            this.status = ExecutionStatus.PENDING;
        }

        public void add(Synchronization synchronization) {
            if (status == ExecutionStatus.FINISHED) {
                // this group of syncs have already ran
                throw new IllegalStateException(ADD_SYNC_ERROR);
            }
            synchs.add(synchronization);
        }

        @Override
        public void beforeCompletion() {
            status = ExecutionStatus.RUNNING;

            // Note that because synchronizations can register other synchronizations
            // we cannot use enhanced for loops as that could cause a concurrency exception
            for (int i = 0; i < synchs.size(); i++) {
                Synchronization sync = synchs.get(i);

                try {
                    sync.beforeCompletion();
                } catch (Exception e) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debugf(
                                "The synchronization %s associated with tx key %s failed during beforeCompletion: %s",
                                sync, tsr.getTransactionKey(), e.getMessage());
                    }

                    if (deferredThrowable == null) {
                        // only save the first failure
                        deferredThrowable = e;
                    }
                }
            }

            status = ExecutionStatus.FINISHED;
        }

        @Override
        public void afterCompletion(int status) {
            // The list should be iterated in reverse order
            for (int i = synchs.size(); i-- > 0;) {
                synchs.get(i).afterCompletion(status);
            }
        }

        // does packageName belong to this group of synchronizations
        private boolean shouldAdd(String packageName) {
            return !packagePrefix.isEmpty() && packageName.startsWith(packagePrefix);
        }
    }

    public AgroalOrderedLastSynchronizationList(
            TransactionSynchronizationRegistryWrapper transactionSynchronizationRegistryWrapper) {

        this.tsr = transactionSynchronizationRegistryWrapper;

        for (var packagePrefix : PKG_PREFIXES) {
            var synchronizationGroup = new SynchronizationGroup(packagePrefix);

            synchGroups.add(synchronizationGroup);

            if (packagePrefix.isEmpty()) {
                otherSynchs = synchronizationGroup; // the catch-all group
            }
        }
    }

    /**
     * Register an interposed synchronization. Note that synchronizations are not allowed if:
     * <p>
     *
     * @param synchronization The synchronization to register
     * @throws IllegalStateException if the transaction is in the wrong state:
     *         <ol>
     *         <li>the transaction has already prepared;
     *         <li>the transaction is marked rollback only
     *         <li>the group that the synchronization should belong to has already been processed
     *         </ol>
     */
    public void registerInterposedSynchronization(Synchronization synchronization) {
        int status = tsr.getTransactionStatus();

        switch (status) {
            case Status.STATUS_ACTIVE:
            case Status.STATUS_PREPARING:
                break;
            default:
                throw new IllegalStateException(REGISTER_SYNC_ERROR + status);
        }

        // add the synchronization to the group that matches this package and, if there is no such group
        // then add it to the catch-all group (otherSyncs)
        String packageName = synchronization.getClass().getName();
        SynchronizationGroup synchGroup = otherSynchs;

        for (SynchronizationGroup g : synchGroups) {
            if (g.shouldAdd(packageName)) {
                synchGroup = g;
                break;
            }
        }

        synchGroup.add(synchronization);
    }

    /**
     * Exceptions from beforeCompletion Synchronizations are not caught because such errors should cause the
     * transaction to roll back.
     */
    @Override
    public void beforeCompletion() {
        // run each group of synchs according to the order they were added to the list
        for (SynchronizationGroup g : synchGroups) {
            g.beforeCompletion();
        }

        if (deferredThrowable != null) {
            /*
             * If any Synchronization threw an exception then only report the first one.
             *
             * Cause the transaction to rollback. The underlying transaction manager will catch the runtime
             * exception and re-throw it when it does the rollback
             */
            throw new RuntimeException(deferredThrowable);
        }
    }

    @Override
    public void afterCompletion(int status) {
        // run each group of synchs according to the order they were added to the list
        for (SynchronizationGroup g : synchGroups) {
            g.afterCompletion(status);
        }
    }
}
