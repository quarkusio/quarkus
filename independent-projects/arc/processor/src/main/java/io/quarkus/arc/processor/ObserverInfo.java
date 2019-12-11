package io.quarkus.arc.processor;

import io.quarkus.arc.processor.BuildExtension.BuildContext;
import io.quarkus.arc.processor.ObserverTransformer.ObserverTransformation;
import io.quarkus.arc.processor.ObserverTransformer.TransformationContext;
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
import org.jboss.jandex.AnnotationTarget;
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

    static ObserverInfo create(BeanInfo declaringBean, MethodInfo observerMethod, Injection injection, boolean isAsync,
            List<ObserverTransformer> transformers, BuildContext buildContext) {
        // Initialize attributes
        MethodParameterInfo eventParameter = initEventParam(observerMethod, declaringBean.getDeployment());
        Type observedType = observerMethod.parameters().get(eventParameter.position());
        Set<AnnotationInstance> qualifiers = initQualifiers(declaringBean.getDeployment(), observerMethod, eventParameter);
        Reception reception = initReception(isAsync, declaringBean.getDeployment(), observerMethod);
        TransactionPhase transactionPhase = initTransactionPhase(isAsync, declaringBean.getDeployment(), observerMethod);
        AnnotationInstance priorityAnnotation = observerMethod.annotation(DotNames.PRIORITY);
        Integer priority;
        if (priorityAnnotation != null && priorityAnnotation.target().equals(eventParameter)) {
            priority = priorityAnnotation.value().asInt();
        } else {
            priority = ObserverMethod.DEFAULT_PRIORITY;
        }

        if (!transformers.isEmpty()) {
            // Transform attributes if needed
            ObserverTransformationContext context = new ObserverTransformationContext(buildContext, observerMethod,
                    observedType, qualifiers, reception, transactionPhase, priority, isAsync);

            for (ObserverTransformer transformer : transformers) {
                if (transformer.appliesTo(observedType, qualifiers)) {
                    transformer.transform(context);
                    if (context.vetoed) {
                        LOGGER.debugf("Observer method %s.%s() vetoed by %s", observerMethod.declaringClass().name(),
                                observerMethod.name(), transformer.getClass().getName());
                        break;
                    }
                }
            }
            if (context.vetoed) {
                // Veto the observer method
                return null;
            }
            qualifiers = context.getQualifiers();
            reception = context.getReception();
            transactionPhase = context.getTransactionPhase();
            priority = context.getPriority();
            isAsync = context.isAsync();
        }

        if (!TransactionPhase.IN_PROGRESS.equals(transactionPhase)) {
            final ClassInfo clazz = observerMethod.declaringClass();
            LOGGER.warnf("The method %s#%s makes use of '%s' transactional observers which are not implemented yet.", clazz,
                    observerMethod.name(), transactionPhase);
        }

        // Create an immutable observer metadata
        return new ObserverInfo(declaringBean, observerMethod, injection, eventParameter, isAsync, priority, reception,
                transactionPhase, qualifiers);

    }

    private final BeanInfo declaringBean;

    private final MethodInfo observerMethod;

    private final Injection injection;

    private final MethodParameterInfo eventParameter;

    private final int eventMetadataParameterPosition;

    private final int priority;

    private final boolean isAsync;

    private final Reception reception;

    private final TransactionPhase transactionPhase;

    private final Set<AnnotationInstance> qualifiers;

    ObserverInfo(BeanInfo declaringBean, MethodInfo observerMethod, Injection injection, MethodParameterInfo eventParameter,
            boolean isAsync, int priority, Reception reception, TransactionPhase transactionPhase,
            Set<AnnotationInstance> qualifiers) {
        this.declaringBean = declaringBean;
        this.observerMethod = observerMethod;
        this.injection = injection;
        this.eventParameter = eventParameter;
        this.eventMetadataParameterPosition = initEventMetadataParam(observerMethod);
        this.isAsync = isAsync;
        this.priority = priority;
        this.reception = reception;
        this.transactionPhase = transactionPhase;
        this.qualifiers = qualifiers;
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

    public Reception getReception() {
        return reception;
    }

    public TransactionPhase getTransactionPhase() {
        return transactionPhase;
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

    public Type getObservedType() {
        return observerMethod.parameters().get(eventParameter.position());
    }

    public Set<AnnotationInstance> getQualifiers() {
        return qualifiers;
    }

    void init(List<Throwable> errors) {
        for (InjectionPointInfo injectionPoint : injection.injectionPoints) {
            Beans.resolveInjectionPoint(declaringBean.getDeployment(), this, injectionPoint, errors);
        }
    }

    static Reception initReception(boolean isAsync, BeanDeployment beanDeployment, MethodInfo observerMethod) {
        AnnotationInstance observesAnnotation = isAsync
                ? beanDeployment.getAnnotation(observerMethod, DotNames.OBSERVES_ASYNC)
                : beanDeployment.getAnnotation(observerMethod, DotNames.OBSERVES);
        AnnotationValue receptionValue = observesAnnotation.value("notifyObserver");
        if (receptionValue == null) {
            return Reception.ALWAYS;
        }
        return Reception.valueOf(receptionValue.asEnum());
    }

    static TransactionPhase initTransactionPhase(boolean isAsync, BeanDeployment beanDeployment, MethodInfo observerMethod) {
        AnnotationInstance observesAnnotation = isAsync
                ? beanDeployment.getAnnotation(observerMethod, DotNames.OBSERVES_ASYNC)
                : beanDeployment.getAnnotation(observerMethod, DotNames.OBSERVES);
        AnnotationValue duringValue = observesAnnotation.value("during");
        if (duringValue == null) {
            return TransactionPhase.IN_PROGRESS;
        }
        return TransactionPhase.valueOf(duringValue.asEnum());
    }

    static Set<AnnotationInstance> initQualifiers(BeanDeployment beanDeployment, MethodInfo observerMethod,
            MethodParameterInfo eventParameter) {
        Set<AnnotationInstance> qualifiers = new HashSet<>();
        for (AnnotationInstance annotation : beanDeployment.getAnnotations(observerMethod)) {
            if (annotation.target().equals(eventParameter)
                    && beanDeployment.getQualifier(annotation.name()) != null) {
                qualifiers.add(annotation);
            }
        }
        return qualifiers;
    }

    int getPriority() {
        return priority;
    }

    static MethodParameterInfo initEventParam(MethodInfo observerMethod, BeanDeployment beanDeployment) {
        List<MethodParameterInfo> eventParams = new ArrayList<>();
        for (AnnotationInstance annotation : beanDeployment.getAnnotations(observerMethod)) {
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

    private static class ObserverTransformationContext extends AnnotationsTransformationContext<Set<AnnotationInstance>>
            implements TransformationContext {

        private final Type observedType;
        private Reception reception;
        private TransactionPhase transactionPhase;
        private Integer priority;
        private boolean vetoed;
        private boolean async;

        public ObserverTransformationContext(BuildContext buildContext, AnnotationTarget target,
                Type observedType, Set<AnnotationInstance> qualifiers, Reception reception, TransactionPhase transactionPhase,
                Integer priority, boolean async) {
            super(buildContext, target, qualifiers);
            this.observedType = observedType;
            this.reception = reception;
            this.transactionPhase = transactionPhase;
            this.priority = priority;
            this.async = async;
        }

        @Override
        public MethodInfo getMethod() {
            return getTarget().asMethod();
        }

        @Override
        public Type getObservedType() {
            return observedType;
        }

        @Override
        public Set<AnnotationInstance> getQualifiers() {
            return getAnnotations();
        }

        @Override
        public Reception getReception() {
            return reception;
        }

        @Override
        public TransactionPhase getTransactionPhase() {
            return transactionPhase;
        }

        public Integer getPriority() {
            return priority;
        }

        @Override
        public boolean isAsync() {
            return async;
        }

        @Override
        public void veto() {
            this.vetoed = true;
        }

        @Override
        public ObserverTransformation transform() {
            return new ObserverTransformationImpl(new HashSet<>(getAnnotations()), getMethod(), this);
        }

    }

    private static class ObserverTransformationImpl
            extends AbstractAnnotationsTransformation<ObserverTransformation, Set<AnnotationInstance>>
            implements ObserverTransformation {

        private final ObserverTransformationContext context;
        private Integer priority;
        private Reception reception;
        private TransactionPhase transactionPhase;
        private Boolean async;

        public ObserverTransformationImpl(Set<AnnotationInstance> qualifiers, MethodInfo observerMethod,
                ObserverTransformationContext context) {
            super(qualifiers, observerMethod, null);
            this.context = context;
        }

        @Override
        protected ObserverTransformation self() {
            return this;
        }

        @Override
        public ObserverTransformation priority(int priority) {
            this.priority = priority;
            return this;
        }

        @Override
        public ObserverTransformation reception(Reception reception) {
            this.reception = reception;
            return this;
        }

        @Override
        public ObserverTransformation transactionPhase(TransactionPhase transactionPhase) {
            this.transactionPhase = transactionPhase;
            return this;
        }
        
        @Override
        public ObserverTransformation async(boolean value) {
            this.async = value;
            return this;
        }

        @Override
        public void done() {
            context.setAnnotations(modifiedAnnotations);
            if (reception != null) {
                context.reception = reception;
            }
            if (priority != null) {
                context.priority = priority;
            }
            if (transactionPhase != null) {
                context.transactionPhase = transactionPhase;
            }
            if (async != null) {
                context.async = async;
            }
        }

    }

}
