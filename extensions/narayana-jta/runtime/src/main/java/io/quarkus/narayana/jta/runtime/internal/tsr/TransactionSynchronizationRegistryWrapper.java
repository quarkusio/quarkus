package io.quarkus.narayana.jta.runtime.internal.tsr;

import jakarta.transaction.Synchronization;
import jakarta.transaction.TransactionSynchronizationRegistry;

import org.jboss.logging.Logger;

import com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionSynchronizationRegistryImple;

/**
 * Agroal registers an interposed synchronization which validates that connections have been released.
 * Components such as hibernate release connections in an interposed synchronization.
 * Therefore, we must ensure that Agroal runs last.
 * <p>
 *
 * This wrapper re-orders interposed synchronizations as follows: [other, hibernate-orm, agroal].
 * <p>
 *
 * Synchronizations are placed into groups according to their package name and the groups are ordered which means
 * that all hibernate synchronizations run before Agroal ones and all other synchs run before the hibernate ones.
 * <p>
 *
 * See {@code AgroalOrderedLastSynchronizationList} for details of the re-ordering.
 */
public class TransactionSynchronizationRegistryWrapper implements TransactionSynchronizationRegistry {
    private final Object key = new Object();
    private static final Logger LOG = Logger.getLogger(TransactionSynchronizationRegistryWrapper.class);

    private final TransactionSynchronizationRegistryImple tsr;
    private transient com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionManagerImple delegate;

    public TransactionSynchronizationRegistryWrapper(
            TransactionSynchronizationRegistryImple transactionSynchronizationRegistryImple) {
        this.tsr = transactionSynchronizationRegistryImple;
    }

    @Override
    public void registerInterposedSynchronization(Synchronization sync) {
        AgroalOrderedLastSynchronizationList agroalOrderedLastSynchronization = (AgroalOrderedLastSynchronizationList) tsr
                .getResource(key);

        if (agroalOrderedLastSynchronization == null) {
            synchronized (key) {
                agroalOrderedLastSynchronization = (AgroalOrderedLastSynchronizationList) tsr.getResource(key);
                if (agroalOrderedLastSynchronization == null) {
                    agroalOrderedLastSynchronization = new AgroalOrderedLastSynchronizationList(this);

                    tsr.putResource(key, agroalOrderedLastSynchronization);
                    tsr.registerInterposedSynchronization(agroalOrderedLastSynchronization);
                }
            }
        }

        // add the synchronization to the list that does the reordering
        agroalOrderedLastSynchronization.registerInterposedSynchronization(sync);
    }

    @Override
    public Object getTransactionKey() {
        return tsr.getTransactionKey();
    }

    @Override
    public int getTransactionStatus() {
        return tsr.getTransactionStatus();
    }

    @Override
    public boolean getRollbackOnly() {
        return tsr.getRollbackOnly();
    }

    @Override
    public void setRollbackOnly() {
        tsr.setRollbackOnly();
    }

    @Override
    public Object getResource(Object key) {
        return tsr.getResource(key);
    }

    @Override
    public void putResource(Object key, Object value) {
        tsr.putResource(key, value);
    }
}
