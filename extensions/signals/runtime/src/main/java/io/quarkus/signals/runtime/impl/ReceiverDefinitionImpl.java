package io.quarkus.signals.runtime.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import jakarta.enterprise.inject.spi.BeanContainer;
import jakarta.enterprise.util.TypeLiteral;

import org.jboss.logging.Logger;

import io.quarkus.signals.Receivers;
import io.quarkus.signals.Receivers.Registration;
import io.quarkus.signals.SignalContext;
import io.quarkus.signals.spi.Receiver;
import io.quarkus.signals.spi.Receiver.ExecutionModel;
import io.smallrye.mutiny.Uni;

class ReceiverDefinitionImpl<SIGNAL, RESPONSE> implements Receivers.ReceiverDefinition<SIGNAL, RESPONSE> {

    private final Function<CallbackReceiver<SIGNAL, RESPONSE>, Receivers.Registration> registerFun;
    private final BeanContainer beanContainer;
    private final Type signalType;
    private Type responseType;
    private Set<Annotation> qualifiers = Set.of();
    private ExecutionModel executionModel = ExecutionModel.BLOCKING;

    public ReceiverDefinitionImpl(Type signalType, BeanContainer beanContainer,
            Function<CallbackReceiver<SIGNAL, RESPONSE>, Receivers.Registration> registerFun) {
        this.signalType = signalType;
        this.beanContainer = beanContainer;
        this.responseType = null;
        this.registerFun = registerFun;
    }

    @Override
    public Receivers.ReceiverDefinition<SIGNAL, RESPONSE> setQualifiers(Annotation... qualifiers) {
        for (Annotation qualifier : qualifiers) {
            if (!beanContainer.isQualifier(qualifier.annotationType())) {
                throw new IllegalArgumentException("Not a qualifier: " + qualifier.annotationType().getName());
            }
        }
        this.qualifiers = Set.of(qualifiers);
        return this;
    }

    @Override
    public Receivers.ReceiverDefinition<SIGNAL, RESPONSE> setExecutionModel(ExecutionModel executionModel) {
        this.executionModel = Objects.requireNonNull(executionModel);
        return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R> Receivers.ReceiverDefinition<SIGNAL, R> setResponseType(Class<R> responseType) {
        this.responseType = Objects.requireNonNull(responseType);
        return (Receivers.ReceiverDefinition<SIGNAL, R>) this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R> Receivers.ReceiverDefinition<SIGNAL, R> setResponseType(TypeLiteral<R> responseType) {
        this.responseType = Objects.requireNonNull(responseType.getType());
        return (Receivers.ReceiverDefinition<SIGNAL, R>) this;
    }

    @Override
    public Registration notify(Function<SignalContext<SIGNAL>, Uni<RESPONSE>> callback) {
        Objects.requireNonNull(callback);
        return registerFun.apply(new CallbackReceiver<>(signalType, qualifiers, responseType, executionModel, callback));
    }

    static class CallbackReceiver<SIGNAL, RESPONSE> implements Receiver<SIGNAL, RESPONSE> {

        private static final Logger LOG = Logger.getLogger(CallbackReceiver.class);

        private final String id;
        private final Type signalType;
        private final Type responseType;
        private final Set<Annotation> qualifiers;
        private final ExecutionModel executionModel;
        private final Function<SignalContext<SIGNAL>, Uni<RESPONSE>> callback;

        CallbackReceiver(Type signalType, Set<Annotation> qualifiers, Type responseType, ExecutionModel executionModel,
                Function<SignalContext<SIGNAL>, Uni<RESPONSE>> callback) {
            this.id = UUID.randomUUID().toString();
            this.signalType = signalType;
            this.responseType = responseType;
            this.qualifiers = qualifiers;
            this.executionModel = executionModel;
            this.callback = callback;
        }

        String id() {
            return id;
        }

        @Override
        public Type signalType() {
            return signalType;
        }

        @Override
        public Set<Annotation> qualifiers() {
            return qualifiers;
        }

        @Override
        public Type responseType() {
            return responseType;
        }

        @Override
        public ExecutionModel executionModel() {
            return executionModel;
        }

        @Override
        public Uni<RESPONSE> notify(SignalContext<SIGNAL> context) {
            try {
                return callback.apply(context);
            } catch (Throwable e) {
                LOG.warnf("Notification of [%s] failed: %s", this, e);
                return Uni.createFrom().failure(e);
            }
        }

        @Override
        public String toString() {
            return "CallbackReceiver [signalType=" + signalType + ", responseType=" + responseType + ", qualifiers="
                    + qualifiers + ", executionModel=" + executionModel + "]";
        }

    }

}
