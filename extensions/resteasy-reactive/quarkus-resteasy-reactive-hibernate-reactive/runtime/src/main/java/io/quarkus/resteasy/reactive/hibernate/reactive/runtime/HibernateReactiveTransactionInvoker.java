package io.quarkus.resteasy.reactive.hibernate.reactive.runtime;

import java.util.function.BiFunction;

import org.hibernate.reactive.mutiny.Mutiny;
import org.jboss.resteasy.reactive.server.spi.EndpointInvoker;

import io.quarkus.arc.Arc;
import io.smallrye.mutiny.Uni;

public class HibernateReactiveTransactionInvoker implements EndpointInvoker {

    private final int sessionIndex;
    private final int transactionIndex;
    private final EndpointInvoker delegate;

    private volatile Mutiny.SessionFactory sessionFactory;

    public HibernateReactiveTransactionInvoker(int sessionIndex, int transactionIndex, EndpointInvoker delegate) {
        this.sessionIndex = sessionIndex;
        this.transactionIndex = transactionIndex;
        this.delegate = delegate;
    }

    @Override
    public Object invoke(Object instance, Object[] parameters) throws Exception {
        return getSessionFactory().withTransaction(new BiFunction<Mutiny.Session, Mutiny.Transaction, Uni<Object>>() {
            @Override
            public Uni<Object> apply(Mutiny.Session session, Mutiny.Transaction transaction) {
                if (sessionIndex >= 0) {
                    parameters[sessionIndex] = session;
                }
                if (transactionIndex >= 0) {
                    parameters[transactionIndex] = transaction;
                }
                try {
                    return (Uni<Object>) delegate.invoke(instance, parameters);
                } catch (Exception e) {
                    return Uni.createFrom().failure(e);
                }
            }
        });
    }

    public Mutiny.SessionFactory getSessionFactory() {
        if (sessionFactory == null) {
            sessionFactory = Arc.container().instance(Mutiny.SessionFactory.class).get();
        }
        return sessionFactory;
    }
}
