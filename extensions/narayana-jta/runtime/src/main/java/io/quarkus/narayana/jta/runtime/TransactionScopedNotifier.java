package io.quarkus.narayana.jta.runtime;

import java.util.Objects;

import jakarta.enterprise.context.BeforeDestroyed;
import jakarta.enterprise.context.Destroyed;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Event;
import jakarta.transaction.SystemException;
import jakarta.transaction.TransactionScoped;

import com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionImple;

import io.quarkus.arc.Arc;

public abstract class TransactionScopedNotifier {

    private transient Event<TransactionId> initialized;
    private transient Event<TransactionId> beforeDestroyed;
    private transient Event<TransactionId> destroyed;

    void initialized(TransactionId transactionId) {
        if (initialized == null) {
            initialized = Arc.container().beanManager().getEvent()
                    .select(TransactionId.class, Initialized.Literal.of(TransactionScoped.class));
        }
        initialized.fire(transactionId);
    }

    void beforeDestroyed(TransactionId transactionId) {
        if (beforeDestroyed == null) {
            beforeDestroyed = Arc.container().beanManager().getEvent()
                    .select(TransactionId.class, BeforeDestroyed.Literal.of(TransactionScoped.class));
        }
        beforeDestroyed.fire(transactionId);
    }

    void destroyed(TransactionId transactionId) {
        if (destroyed == null) {
            destroyed = Arc.container().beanManager().getEvent()
                    .select(TransactionId.class, Destroyed.Literal.of(TransactionScoped.class));
        }
        destroyed.fire(transactionId);
    }

    TransactionId getTransactionId() throws SystemException {
        try {
            return new TransactionId(TransactionImple.getTransaction().toString());
        } catch (Exception e) {
            throw new SystemException("The transaction is not active!");
        }
    }

    // we use this wrapper because if we fire an event with string payload then any "@Observes String payload" would be notified
    public static final class TransactionId {

        private final String value;

        public TransactionId(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            TransactionId other = (TransactionId) obj;
            return Objects.equals(value, other.value);
        }

    }

}
