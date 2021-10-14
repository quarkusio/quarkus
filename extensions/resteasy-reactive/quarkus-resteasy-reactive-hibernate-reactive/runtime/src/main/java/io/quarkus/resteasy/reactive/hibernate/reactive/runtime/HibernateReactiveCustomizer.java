package io.quarkus.resteasy.reactive.hibernate.reactive.runtime;

import org.jboss.resteasy.reactive.server.handlers.InvocationHandler;
import org.jboss.resteasy.reactive.server.model.HandlerChainCustomizer;
import org.jboss.resteasy.reactive.server.spi.EndpointInvoker;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;

import io.quarkus.runtime.annotations.RecordableConstructor;

public class HibernateReactiveCustomizer implements HandlerChainCustomizer {

    private final int sessionIndex;
    private final int transactionIndex;

    @RecordableConstructor
    public HibernateReactiveCustomizer(int sessionIndex, int transactionIndex) {
        this.sessionIndex = sessionIndex;
        this.transactionIndex = transactionIndex;
    }

    @Override
    public ServerRestHandler alternateInvocationHandler(EndpointInvoker invoker) {
        return new InvocationHandler(new HibernateReactiveTransactionInvoker(sessionIndex, transactionIndex, invoker));
    }

    public int getSessionIndex() {
        return sessionIndex;
    }

    public int getTransactionIndex() {
        return transactionIndex;
    }
}
