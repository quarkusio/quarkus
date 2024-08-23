package io.quarkus.smallrye.context.runtime;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.context.ThreadContext;

import io.quarkus.arc.DefaultBean;
import io.smallrye.context.SmallRyeThreadContext;

@Dependent
public class SmallRyeContextPropagationProvider {

    @Produces
    @Singleton
    @DefaultBean
    public SmallRyeThreadContext getAllThreadContext() {
        // Make sure we use the default values, which use the MP Config keys to allow users to override them
        return (SmallRyeThreadContext) ThreadContext.builder()
                .build();
    }

}
