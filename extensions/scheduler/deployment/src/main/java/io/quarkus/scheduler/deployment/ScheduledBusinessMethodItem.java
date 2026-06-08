package io.quarkus.scheduler.deployment;

import static io.quarkus.arc.processor.Reproducibility.BEAN_COMPARATOR;
import static io.quarkus.arc.processor.Reproducibility.METHOD_COMPARATOR;

import java.util.Comparator;
import java.util.List;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.MethodInfo;

import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.builder.item.MultiBuildItem;

public final class ScheduledBusinessMethodItem extends MultiBuildItem implements Comparable<ScheduledBusinessMethodItem> {

    private final BeanInfo bean;
    private final List<AnnotationInstance> schedules;
    private final MethodInfo method;
    private final boolean nonBlocking;
    private final boolean runOnVirtualThread;

    private static final Comparator<ScheduledBusinessMethodItem> COMPARATOR = Comparator
            .comparing(ScheduledBusinessMethodItem::getBean, Comparator.nullsFirst(BEAN_COMPARATOR))
            .thenComparing(ScheduledBusinessMethodItem::getMethod, METHOD_COMPARATOR);

    public ScheduledBusinessMethodItem(BeanInfo bean, MethodInfo method, List<AnnotationInstance> schedules) {
        this(bean, method, schedules, false, false);
    }

    public ScheduledBusinessMethodItem(BeanInfo bean, MethodInfo method, List<AnnotationInstance> schedules,
            boolean hasNonBlockingAnnotation, boolean hasRunOnVirtualThreadAnnotation) {
        this.bean = bean;
        this.method = method;
        this.schedules = schedules;
        this.nonBlocking = hasNonBlockingAnnotation || SchedulerDotNames.COMPLETION_STAGE.equals(method.returnType().name())
                || SchedulerDotNames.UNI.equals(method.returnType().name()) || KotlinUtil.isSuspendMethod(method);
        this.runOnVirtualThread = hasRunOnVirtualThreadAnnotation;
    }

    /**
     *
     * @return the bean or {@code null} for a static method
     */
    public BeanInfo getBean() {
        return bean;
    }

    public MethodInfo getMethod() {
        return method;
    }

    public List<AnnotationInstance> getSchedules() {
        return schedules;
    }

    public boolean isNonBlocking() {
        return nonBlocking;
    }

    public boolean isRunOnVirtualThread() {
        return runOnVirtualThread;
    }

    public String getMethodDescription() {
        return method.declaringClass().name() + "#" + method.name() + "()";
    }

    @Override
    public int compareTo(ScheduledBusinessMethodItem o) {
        return COMPARATOR.compare(this, o);
    }

}
