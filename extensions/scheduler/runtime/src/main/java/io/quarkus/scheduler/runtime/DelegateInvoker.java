package io.quarkus.scheduler.runtime;

abstract class DelegateInvoker implements ScheduledInvoker {

    protected final ScheduledInvoker delegate;

    public DelegateInvoker(ScheduledInvoker delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean isBlocking() {
        return delegate.isBlocking();
    }

}
