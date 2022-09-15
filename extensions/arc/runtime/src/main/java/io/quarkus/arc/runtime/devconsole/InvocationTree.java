package io.quarkus.arc.runtime.devconsole;

import java.lang.reflect.Method;

import jakarta.enterprise.context.RequestScoped;

import io.quarkus.arc.InjectableBean;

@RequestScoped
public class InvocationTree {

    // The current invocation builder
    // It is volatile because a request scoped bean should not be invoked concurrently, however it can be invoked on a different thread
    private volatile Invocation.Builder current;

    Invocation.Builder invocationStarted(InjectableBean<?> bean, Method method, Invocation.Kind kind) {
        Invocation.Builder builder = this.current;
        if (builder == null) {
            // Entry point
            builder = new Invocation.Builder();
        } else {
            // Nested invocation
            builder = builder.newChild();
        }
        builder.setStart(System.currentTimeMillis()).setInterceptedBean(bean)
                .setMethod(method).setKind(kind);
        this.current = builder;
        return builder;
    }

    void invocationCompleted() {
        Invocation.Builder current = this.current;
        if (current == null) {
            // Something went wrong, for example the request context was terminated unexpectedly
            return;
        }
        if (current.getParent() != null) {
            this.current = current.getParent();
        } else {
            this.current = null;
        }
    }

}
