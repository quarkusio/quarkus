package io.quarkus.arc.processor;

public interface InjectionTargetInfo {

    TargetKind kind();

    default BeanInfo asBean() {
        throw new IllegalStateException("Not a bean");
    }

    default ObserverInfo asObserver() {
        throw new IllegalStateException("Not an observer");
    }

    default DisposerInfo asDisposer() {
        throw new IllegalStateException("Not a disposer");
    }

    enum TargetKind {

        BEAN,
        OBSERVER,
        DISPOSER,

    }

}
