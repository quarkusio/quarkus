package io.quarkus.smallrye.context.runtime;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import org.eclipse.microprofile.context.ThreadContext;

import io.quarkus.arc.DefaultBean;

@Dependent
public class SmallRyeContextPropagationProvider {

    @Produces
    @Singleton
    @DefaultBean
    public ThreadContext getAllThreadContext() {
        return ThreadContext.builder().propagated(ThreadContext.ALL_REMAINING).cleared().unchanged().build();
    }

}
