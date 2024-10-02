package io.quarkus.vertx.runtime;

import jakarta.enterprise.invoke.Invoker;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.RecordableConstructor;
import io.quarkus.vertx.ConsumeEvent;

public class EventConsumerInfo {
    /**
     * The {@link ConsumeEvent} annotation declared on the event consumer method.
     */
    public final ConsumeEvent annotation;

    /**
     * Whether the {@link io.smallrye.common.annotation.Blocking} annotation
     * was declared on the event consumer method.
     */
    public final boolean blockingAnnotation;

    /**
     * Whether the {@link io.smallrye.common.annotation.RunOnVirtualThread} annotation
     * was declared on the event consumer method.
     */
    public final boolean runOnVirtualThreadAnnotation;

    /**
     * Whether the event consumer method declares 2 parameters, where the first
     * is the event headers and the second is the event body. In this case,
     * the {@link io.quarkus.vertx.runtime.EventConsumerInvoker} has to split
     * the headers and body parameters explicitly.
     */
    public final boolean splitHeadersBodyParams;

    /**
     * The {@linkplain Invoker invoker} for the event consumer method.
     */
    public final RuntimeValue<Invoker<Object, Object>> invoker;

    @RecordableConstructor
    public EventConsumerInfo(ConsumeEvent annotation, boolean blockingAnnotation, boolean runOnVirtualThreadAnnotation,
            boolean splitHeadersBodyParams, RuntimeValue<Invoker<Object, Object>> invoker) {
        this.annotation = annotation;
        this.blockingAnnotation = blockingAnnotation;
        this.runOnVirtualThreadAnnotation = runOnVirtualThreadAnnotation;
        this.splitHeadersBodyParams = splitHeadersBodyParams;
        this.invoker = invoker;
    }
}
