package io.quarkus.context.test;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.context.ThreadContext;

@ApplicationScoped
public class ProducerBean {

    @Produces
    @ApplicationScoped
    public ThreadContext getAllThreadContext() {
        return ThreadContext.builder().propagated(ThreadContext.ALL_REMAINING).cleared().unchanged().build();
    }

    @ApplicationScoped
    @Produces
    public ManagedExecutor getAllManagedExecutor() {
        return ManagedExecutor.builder().build();
    }

    @Produces
    public String doIt() {
        return "foo";
    }

    public void disposeExecutor(@Disposes ManagedExecutor me) {
        me.shutdown();
    }
}
