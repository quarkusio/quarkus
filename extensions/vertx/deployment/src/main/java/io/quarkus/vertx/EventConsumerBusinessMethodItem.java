package io.quarkus.vertx;

import org.jboss.builder.item.MultiBuildItem;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.MethodInfo;
import org.jboss.quarkus.arc.processor.BeanInfo;

public final class EventConsumerBusinessMethodItem extends MultiBuildItem {

    private final BeanInfo bean;
    private final AnnotationInstance consumeEvent;
    private final MethodInfo method;

    public EventConsumerBusinessMethodItem(BeanInfo bean, MethodInfo method, AnnotationInstance consumeEvent) {
        this.bean = bean;
        this.method = method;
        this.consumeEvent = consumeEvent;
    }

    public BeanInfo getBean() {
        return bean;
    }

    public MethodInfo getMethod() {
        return method;
    }

    public AnnotationInstance getConsumeEvent() {
        return consumeEvent;
    }

}
