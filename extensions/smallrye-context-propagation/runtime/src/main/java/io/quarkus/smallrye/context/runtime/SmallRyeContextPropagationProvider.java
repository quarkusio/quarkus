package io.quarkus.smallrye.context.runtime;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import org.eclipse.microprofile.context.ThreadContext;

import io.quarkus.arc.DefaultBean;
import io.smallrye.context.SmallRyeThreadContext;

@Dependent
public class SmallRyeContextPropagationProvider {

    @Produces
    @Singleton
    @DefaultBean
    public SmallRyeThreadContext getAllThreadContext() {
        return (SmallRyeThreadContext) ThreadContext.builder().propagated(ThreadContext.ALL_REMAINING).cleared().unchanged()
                .build();
    }

}
