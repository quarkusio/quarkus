package io.quarkus.arc.impl;

import io.quarkus.arc.ContextInstanceHandle;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.InjectableContext.ContextState;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.enterprise.context.spi.Contextual;

class InstanceHandlesContextState implements ContextState {

    private final Collection<ContextInstanceHandle<?>> handles;

    InstanceHandlesContextState(Collection<ContextInstanceHandle<?>> handles) {
        this.handles = handles;
    }

    @Override
    public Map<InjectableBean<?>, Object> getContextualInstances() {
        return handles.stream().collect(Collectors.toMap(ContextInstanceHandle::getBean, ContextInstanceHandle::get));
    }

    ConcurrentMap<Contextual<?>, ContextInstanceHandle<?>> toConcurrentMap() {
        return handles.stream().collect(Collectors.toConcurrentMap(ContextInstanceHandle::getBean, Function.identity()));
    }

}
