package io.quarkus.arc.processor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import javax.enterprise.event.Reception;
import javax.enterprise.event.TransactionPhase;
import javax.enterprise.inject.spi.DefinitionException;
import javax.enterprise.inject.spi.ObserverMethod;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.MethodParameterInfo;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;

/**
 * Represents an observer method.
 *
 * @author Martin Kouba
 */
public class ObserverInfo implements InjectionTargetInfo {
    private static final Logger LOGGER = Logger.getLogger(ObserverInfo.class.getName());
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

    @Override
    public TargetKind kind() {
        return TargetKind.OBSERVER;
    }

    @Override
    public ObserverInfo asObserver() {
        return this;
    }

    public BeanInfo getDeclaringBean() {
        return declaringBean;
    }

    public MethodInfo getObserverMethod() {
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

    public Reception getReception() {
        AnnotationInstance observesAnnotation = isAsync
                ? declaringBean.getDeployment().getAnnotation(observerMethod, DotNames.OBSERVES_ASYNC)
                : declaringBean.getDeployment().getAnnotation(observerMethod, DotNames.OBSERVES);
        AnnotationValue receptionValue = observesAnnotation.value("notifyObserver");
        if (receptionValue == null) {
            return Reception.ALWAYS;
        }
        return Reception.valueOf(receptionValue.asEnum());
    }

    void init(List<Throwable> errors) {
        for (InjectionPointInfo injectionPoint : injection.injectionPoints) {
            Beans.resolveInjectionPoint(declaringBean.getDeployment(), this, injectionPoint, errors);
        }
    }

    public Type getObservedType() {
        return observerMethod.parameters().get(eventParameter.position());
    }

    public Set<AnnotationInstance> getQualifiers() {
        Set<AnnotationInstance> qualifiers = new HashSet<>();
        for (AnnotationInstance annotation : declaringBean.getDeployment().getAnnotations(observerMethod)) {
            if (annotation.target().equals(eventParameter)
                    && declaringBean.getDeployment().getQualifier(annotation.name()) != null) {
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

        for (AnnotationInstance annotation : declaringBean.getDeployment().getAnnotations(observerMethod)) {
            if (Kind.METHOD_PARAMETER == annotation.target().kind()
                    && (annotation.name().equals(DotNames.OBSERVES) || annotation.name().equals(DotNames.OBSERVES_ASYNC))) {
                eventParams.add(annotation.target().asMethodParameter());

                /**
                 * Log a warning for usage of transactional observers other than IN_PROGRESS since they are not implemented yet:
                 * https://github.com/quarkusio/quarkus/issues/2224.
                 */
                final AnnotationValue during = annotation.value("during");
                if (isTransactionalObserver(during) && !isDuringTransactionInProgress(during)) {
                    final ClassInfo clazz = observerMethod.declaringClass();
                    final String transactionalObserverWarning = "The method %s#%s makes use of '%s' transactional observers which are not implemented yet.";
                    LOGGER.warnf(transactionalObserverWarning, clazz, observerMethod.name(), during.asEnum());
                }
            }
        }

        if (eventParams.isEmpty()) {
            throw new DefinitionException("No event parameters found for " + observerMethod);
        } else if (eventParams.size() > 1) {
            throw new DefinitionException("Multiple event parameters found for " + observerMethod);
        }
        return eventParams.get(0);
    }

    private boolean isTransactionalObserver(AnnotationValue during) {
        return during != null && AnnotationValue.Kind.ENUM.equals(during.kind())
                && during.asEnumType().equals(DotNames.TRANSACTION_PHASE);
    }

    private boolean isDuringTransactionInProgress(AnnotationValue during) {
        return TransactionPhase.IN_PROGRESS.name().equals(during.asEnum());
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
