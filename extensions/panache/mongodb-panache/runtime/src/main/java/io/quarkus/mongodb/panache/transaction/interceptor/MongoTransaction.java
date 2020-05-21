package io.quarkus.mongodb.panache.transaction.interceptor;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.xa.XAResource;

import com.mongodb.client.ClientSession;

public class MongoTransaction implements Transaction {
    private ClientSession clientSession;
    private int status = Status.STATUS_ACTIVE;

    public ClientSession getClientSession() {
        return clientSession;
    }

    void setClientSession(ClientSession clientSession) {
        this.clientSession = clientSession;
    }

    @Override
    public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException,
            IllegalStateException, SystemException {
        if (isDone()) {
            throw new IllegalStateException("Transaction is done. Cannot commit transaction.");
        }

        this.status = Status.STATUS_COMMITTING;
        try {
            this.clientSession.commitTransaction();
        } finally {
            this.clientSession.close();
        }
        this.status = Status.STATUS_COMMITTED;
    }

    @Override
    public boolean delistResource(XAResource xaRes, int flag) throws IllegalStateException, SystemException {
        throw new SystemException("XA is not supported");
    }

    @Override
    public boolean enlistResource(XAResource xaRes) throws RollbackException, IllegalStateException, SystemException {
        throw new SystemException("XA is not supported");
    }

    @Override
    public int getStatus() throws SystemException {
        return this.status;
    }

    @Override
    public void registerSynchronization(Synchronization sync) throws RollbackException, IllegalStateException, SystemException {
    }

    @Override
    public void rollback() throws IllegalStateException, SystemException {
        if (isDone()) {
            throw new IllegalStateException("Transaction is done. Cannot rollback transaction.");
        }

        this.status = Status.STATUS_ROLLING_BACK;
        try {
            this.clientSession.abortTransaction();
        } finally {
            this.clientSession.close();
        }
        this.status = Status.STATUS_ROLLEDBACK;
    }

    @Override
    public void setRollbackOnly() throws IllegalStateException, SystemException {
        if (isDone()) {
            throw new IllegalStateException("Transaction is done. Cannot change status");
        }

        this.status = Status.STATUS_MARKED_ROLLBACK;
    }

    private boolean isDone() {
        switch (status) {
            case Status.STATUS_PREPARING:
            case Status.STATUS_PREPARED:
            case Status.STATUS_COMMITTING:
            case Status.STATUS_COMMITTED:
            case Status.STATUS_ROLLING_BACK:
            case Status.STATUS_ROLLEDBACK:
            case Status.STATUS_UNKNOWN:
                return true;
        }
        return false;
    }
}
