package org.jboss.protean.arc;

import java.util.Collection;

public final class Components {

    private final Collection<InjectableBean<?>> beans;

    private final Collection<InjectableObserverMethod<?>> observers;

    public Components(Collection<InjectableBean<?>> beans, Collection<InjectableObserverMethod<?>> observers) {
        this.beans = beans;
        this.observers = observers;
    }

    public Collection<InjectableBean<?>> getBeans() {
        return beans;
    }

    public Collection<InjectableObserverMethod<?>> getObservers() {
        return observers;
    }

}
