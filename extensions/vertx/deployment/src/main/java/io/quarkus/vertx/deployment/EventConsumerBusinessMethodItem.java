package io.quarkus.vertx.deployment;

import org.jboss.jandex.AnnotationInstance;

import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.arc.processor.InvokerInfo;
import io.quarkus.builder.item.MultiBuildItem;

public final class EventConsumerBusinessMethodItem extends MultiBuildItem {

    private final BeanInfo bean;
    private final AnnotationInstance consumeEvent;
    private final boolean blockingAnnotation;
    private final boolean runOnVirtualThreadAnnotation;
    private final boolean splitHeadersBodyParams;
    private final InvokerInfo invoker;

    public EventConsumerBusinessMethodItem(BeanInfo bean, AnnotationInstance consumeEvent, boolean blockingAnnotation,
            boolean runOnVirtualThreadAnnotation, boolean splitHeadersBodyParams, InvokerInfo invoker) {
        this.bean = bean;
        this.consumeEvent = consumeEvent;
        this.blockingAnnotation = blockingAnnotation;
        this.runOnVirtualThreadAnnotation = runOnVirtualThreadAnnotation;
        this.splitHeadersBodyParams = splitHeadersBodyParams;
        this.invoker = invoker;
    }

    /**
     * Returns the bean that declares this event consumer method.
     */
    public BeanInfo getBean() {
        return bean;
    }

    /**
     * Returns the {@link io.quarkus.vertx.ConsumeEvent} annotation declared
     * on this event consumer method.
     */
    public AnnotationInstance getConsumeEvent() {
        return consumeEvent;
    }

    /**
     * Returns whether this event consumer method declares
     * the {@link io.smallrye.common.annotation.Blocking} annotation.
     */
    public boolean isBlockingAnnotation() {
        return blockingAnnotation;
    }

    /**
     * Returns whether this event consumer method declares
     * the {@link io.smallrye.common.annotation.RunOnVirtualThread} annotation.
     */
    public boolean isRunOnVirtualThreadAnnotation() {
        return runOnVirtualThreadAnnotation;
    }

    /**
     * Returns whether this event consumer method declares 2 parameters,
     * where the first is the event headers and the second is the event body.
     * In this case, the {@link io.quarkus.vertx.runtime.EventConsumerInvoker}
     * has to split the headers and body parameters explicitly.
     */
    public boolean isSplitHeadersBodyParams() {
        return splitHeadersBodyParams;
    }

    /**
     * Returns the {@linkplain InvokerInfo invoker} for this event consumer method.
     */
    public InvokerInfo getInvoker() {
        return invoker;
    }

}
