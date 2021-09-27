package io.quarkus.narayana.jta.runtime.context;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionScoped;
import javax.transaction.TransactionSynchronizationRegistry;

import com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionSynchronizationRegistryImple;

import io.quarkus.arc.ContextInstanceHandle;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.InjectableContext;
import io.quarkus.arc.impl.ContextInstanceHandleImpl;
import io.quarkus.arc.impl.LazyValue;

/**
 * {@link javax.enterprise.context.spi.Context} class which defines the {@link TransactionScoped} context.
 */
public class TransactionContext implements InjectableContext {
    // marker object to be put as a key for SynchronizationRegistry to gather all beans created in the scope
    private static final Object TRANSACTION_CONTEXT_MARKER = new Object();

    private final LazyValue<TransactionSynchronizationRegistry> transactionSynchronizationRegistry = new LazyValue<>(
            new Supplier<TransactionSynchronizationRegistry>() {
                @Override
                public TransactionSynchronizationRegistry get() {
                    return new TransactionSynchronizationRegistryImple();
                }
            });
    private final LazyValue<TransactionManager> transactionManager = new LazyValue<>(new Supplier<TransactionManager>() {
        @Override
        public TransactionManager get() {
            return com.arjuna.ats.jta.TransactionManager.transactionManager();
        }
    });

    @Override
    public void destroy() {
        if (!isActive()) {
            return;
        }

        TransactionContextState contextState = (TransactionContextState) transactionSynchronizationRegistry.get()
                .getResource(TRANSACTION_CONTEXT_MARKER);
        if (contextState == null) {
            return;
        }
        contextState.destroy();
    }

    @Override
    public void destroy(Contextual<?> contextual) {
        if (!isActive()) {
            return;
        }
        TransactionContextState contextState = (TransactionContextState) transactionSynchronizationRegistry.get()
                .getResource(TRANSACTION_CONTEXT_MARKER);
        if (contextState == null) {
            return;
        }
        contextState.remove(contextual);
    }

    @Override
    public ContextState getState() {
        if (!isActive()) {
            throw new ContextNotActiveException("No active transaction on the current thread");
        }

        ContextState result;
        TransactionContextState contextState = (TransactionContextState) transactionSynchronizationRegistry.get()
                .getResource(TRANSACTION_CONTEXT_MARKER);
        if (contextState == null) {
            result = new TransactionContextState(getCurrentTransaction());
        } else {
            result = contextState;
        }
        return result;
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return TransactionScoped.class;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(Contextual<T> contextual, CreationalContext<T> creationalContext) {
        if (!isActive()) {
            throw new ContextNotActiveException();
        }
        if (contextual == null) {
            throw new IllegalArgumentException("Contextual parameter must not be null");
        }

        TransactionContextState contextState;
        contextState = (TransactionContextState) transactionSynchronizationRegistry.get()
                .getResource(TRANSACTION_CONTEXT_MARKER);

        if (contextState == null) {
            contextState = new TransactionContextState(getCurrentTransaction());
            transactionSynchronizationRegistry.get().putResource(TRANSACTION_CONTEXT_MARKER, contextState);
        }

        ContextInstanceHandle<T> instanceHandle = contextState.get(contextual);
        if (instanceHandle != null) {
            return instanceHandle.get();
        } else if (creationalContext != null) {
            T createdInstance = contextual.create(creationalContext);
            instanceHandle = new ContextInstanceHandleImpl<>((InjectableBean<T>) contextual, createdInstance,
                    creationalContext);

            contextState.put(contextual, instanceHandle);

            return createdInstance;
        } else {
            return null;
        }
    }

    @Override
    public <T> T get(Contextual<T> contextual) {
        return get(contextual, null);
    }

    /**
     * The transaction scoped context is active when a transaction is active.
     */
    @Override
    public boolean isActive() {
        Transaction transaction = getCurrentTransaction();
        if (transaction == null) {
            return false;
        }

        try {
            int currentStatus = transaction.getStatus();
            return currentStatus == Status.STATUS_ACTIVE ||
                    currentStatus == Status.STATUS_MARKED_ROLLBACK ||
                    currentStatus == Status.STATUS_PREPARED ||
                    currentStatus == Status.STATUS_UNKNOWN ||
                    currentStatus == Status.STATUS_PREPARING ||
                    currentStatus == Status.STATUS_COMMITTING ||
                    currentStatus == Status.STATUS_ROLLING_BACK;
        } catch (SystemException e) {
            throw new RuntimeException("Error getting the status of the current transaction", e);
        }
    }

    private Transaction getCurrentTransaction() {
        try {
            return transactionManager.get().getTransaction();
        } catch (SystemException e) {
            throw new RuntimeException("Error getting the current transaction", e);
        }
    }

    /**
     * Representing of the context state. It's a container for all available beans in the context.
     * It's filled during bean usage and cleared on destroy.
     */
    private static class TransactionContextState implements ContextState, Synchronization {

        private final ConcurrentMap<Contextual<?>, ContextInstanceHandle<?>> mapBeanToInstanceHandle = new ConcurrentHashMap<>();

        TransactionContextState(Transaction transaction) {
            try {
                transaction.registerSynchronization(this);
            } catch (RollbackException | SystemException e) {
                throw new RuntimeException("Cannot register synchronization", e);
            }
        }

        /**
         * Put the contextual bean and its handle to the container.
         *
         * @param bean bean to be added
         * @param handle handle for the bean which incorporates the bean, contextual instance and the context
         */
        <T> void put(Contextual<T> bean, ContextInstanceHandle<T> handle) {
            mapBeanToInstanceHandle.put(bean, handle);
        }

        /**
         * Remove the bean from the container.
         *
         * @param bean contextual bean instance
         */
        <T> void remove(Contextual<T> bean) {
            ContextInstanceHandle<?> instance = mapBeanToInstanceHandle.remove(bean);
            if (instance != null) {
                instance.destroy();
            }
        }

        /**
         * Retrieve the bean saved in the container.
         *
         * @param bean retrieving the bean from the container, otherwise {@code null} is returned
         */
        <T> ContextInstanceHandle<T> get(Contextual<T> bean) {
            return (ContextInstanceHandle<T>) mapBeanToInstanceHandle.get(bean);
        }

        /**
         * Destroying all the beans in the container and clearing the container.
         */
        void destroy() {
            for (ContextInstanceHandle<?> handle : mapBeanToInstanceHandle.values()) {
                handle.destroy();
            }
            mapBeanToInstanceHandle.clear();
        }

        /**
         * Method required by the {@link io.quarkus.arc.InjectableContext.ContextState} interface
         * which is then used to get state of the scope in method {@link InjectableContext#getState()}
         *
         * @return list of context bean and the bean instances which are available in the container
         */
        @Override
        public Map<InjectableBean<?>, Object> getContextualInstances() {
            return mapBeanToInstanceHandle.values().stream()
                    .collect(Collectors.toMap(ContextInstanceHandle::getBean, ContextInstanceHandle::get));
        }

        @Override
        public void beforeCompletion() {
        }

        @Override
        public void afterCompletion(int status) {
            this.destroy();
        }
    }
}
