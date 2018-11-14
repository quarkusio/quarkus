package org.jboss.protean.arc.processor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import javax.enterprise.inject.spi.DefinitionException;
import javax.enterprise.inject.spi.ObserverMethod;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.MethodParameterInfo;
import org.jboss.jandex.Type;

/**
 * Represents an observer method.
 *
 * @author Martin Kouba
 */
public class ObserverInfo {

    private final BeanInfo declaringBean;

    private final MethodInfo observerMethod;

    private final Injection injection;

    private final MethodParameterInfo eventParameter;

    private final int eventMetadataParameterPosition;

    private final int priority;

    private final boolean isAsync;

    ObserverInfo(BeanInfo declaringBean, MethodInfo observerMethod, Injection injection, boolean isAsync) {
        this.declaringBean = declaringBean;
        this.observerMethod = observerMethod;
        this.injection = injection;
        this.eventParameter = initEventParam(observerMethod);
        this.eventMetadataParameterPosition = initEventMetadataParam(observerMethod);
        AnnotationInstance priorityAnnotation = observerMethod.annotation(DotNames.PRIORITY);
        if (priorityAnnotation != null && priorityAnnotation.target().equals(eventParameter)) {
            this.priority = priorityAnnotation.value().asInt();
        } else {
            this.priority = ObserverMethod.DEFAULT_PRIORITY;
        }
        this.isAsync = isAsync;
    }

    public BeanInfo getDeclaringBean() {
        return declaringBean;
    }

    MethodInfo getObserverMethod() {
        return observerMethod;
    }

    public MethodParameterInfo getEventParameter() {
        return eventParameter;
    }

    int getEventMetadataParameterPosition() {
        return eventMetadataParameterPosition;
    }

    Injection getInjection() {
        return injection;
    }

    public boolean isAsync() {
        return isAsync;
    }

    void init() {
        for (InjectionPointInfo injectionPoint : injection.injectionPoints) {
            Beans.resolveInjectionPoint(declaringBean.getDeployment(), null, injectionPoint);
        }
    }

    public Type getObservedType() {
        return observerMethod.parameters().get(eventParameter.position());
    }

    public Set<AnnotationInstance> getQualifiers() {
        Set<AnnotationInstance> qualifiers = new HashSet<>();
        for (AnnotationInstance annotation : observerMethod.annotations()) {
            if (annotation.target().equals(eventParameter) && declaringBean.getDeployment().getQualifier(annotation.name()) != null) {
                qualifiers.add(annotation);
            }
        }
        return qualifiers;
    }

    int getPriority() {
        return priority;
    }

    MethodParameterInfo initEventParam(MethodInfo observerMethod) {
        List<MethodParameterInfo> eventParams = new ArrayList<>();
        for (AnnotationInstance annotation : observerMethod.annotations()) {
            if (Kind.METHOD_PARAMETER == annotation.target().kind()
                    && (annotation.name().equals(DotNames.OBSERVES) || annotation.name().equals(DotNames.OBSERVES_ASYNC))) {
                eventParams.add(annotation.target().asMethodParameter());
            }
        }
        if (eventParams.isEmpty()) {
            throw new DefinitionException("No event parameters found for " + observerMethod);
        } else if (eventParams.size() > 1) {
            throw new DefinitionException("Multiple event parameters found for " + observerMethod);
        }
        return eventParams.get(0);
    }

    int initEventMetadataParam(MethodInfo observerMethod) {
        for (ListIterator<Type> iterator = observerMethod.parameters().listIterator(); iterator.hasNext();) {
            if (iterator.next().name().equals(DotNames.EVENT_METADATA)) {
                return iterator.previousIndex();
            }
        }
        return -1;
    }

}
