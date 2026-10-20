package io.quarkus.smallrye.context.runtime;

import org.eclipse.microprofile.context.ThreadContext;

import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.SyntheticCreationalContext;

public class ConfiguredThreadContextCreator implements BeanCreator<ThreadContext> {

    public static final String CLEARED = "cleared";
    public static final String PROPAGATED = "propagated";
    public static final String UNCHANGED = "unchanged";

    @Override
    public ThreadContext create(SyntheticCreationalContext<ThreadContext> context) {
        return ThreadContext.builder()
                .propagated((String[]) context.getParams().get(PROPAGATED))
                .cleared((String[]) context.getParams().get(CLEARED))
                .unchanged((String[]) context.getParams().get(UNCHANGED))
                .build();
    }
}
