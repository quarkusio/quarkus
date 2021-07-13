package io.quarkus.arc.processor;

import static io.quarkus.arc.processor.Annotations.find;
import static io.quarkus.arc.processor.Annotations.getParameterAnnotations;

import io.quarkus.arc.processor.BuildExtension.BuildContext;
import io.quarkus.arc.processor.ObserverTransformer.ObserverTransformation;
import io.quarkus.arc.processor.ObserverTransformer.TransformationContext;
import io.quarkus.gizmo.MethodCreator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.function.Consumer;
import javax.enterprise.event.Reception;
import javax.enterprise.event.TransactionPhase;
import javax.enterprise.inject.spi.DefinitionException;
import javax.enterprise.inject.spi.ObserverMethod;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
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
            List<ObserverTransformer> transformers, BuildContext buildContext, boolean jtaCapabilities) {
        MethodParameterInfo eventParameter = initEventParam(observerMethod, declaringBean.getDeployment());
        AnnotationInstance priorityAnnotation = find(
                getParameterAnnotations(declaringBean.getDeployment(), observerMethod, eventParameter.position()),
                DotNames.PRIORITY);
        Integer priority;
        if (priorityAnnotation != null) {
            priority = priorityAnnotation.value().asInt();
        } else {
            priority = ObserverMethod.DEFAULT_PRIORITY;
        }
        return create(null, declaringBean.getDeployment(), declaringBean.getTarget().get().asClass().name(), declaringBean,
                observerMethod, injection,
                eventParameter,
                observerMethod.parameters().get(eventParameter.position()),
                initQualifiers(declaringBean.getDeployment(), observerMethod, eventParameter),
                initReception(isAsync, declaringBean.getDeployment(), observerMethod),
                initTransactionPhase(isAsync, declaringBean.getDeployment(), observerMethod), isAsync, priority, transformers,
                buildContext, jtaCapabilities, null);
    }

    static ObserverInfo create(String id, BeanDeployment beanDeployment, DotName beanClass, BeanInfo declaringBean,
            MethodInfo observerMethod, Injection injection,
            MethodParameterInfo eventParameter, Type observedType, Set<AnnotationInstance> qualifiers, Reception reception,
            TransactionPhase transactionPhase, boolean isAsync, int priority,
            List<ObserverTransformer> transformers, BuildContext buildContext, boolean jtaCapabilities,
            Consumer<MethodCreator> notify) {

        if (!transformers.isEmpty()) {
            // Transform attributes if needed
            ObserverTransformationContext context = new ObserverTransformationContext(buildContext, observerMethod,
                    observedType, qualifiers, reception, transactionPhase, priority, isAsync);
            for (ObserverTransformer transformer : transformers) {
                if (transformer.appliesTo(observedType, qualifiers)) {
                    transformer.transform(context);
                    if (context.vetoed) {
                        String info;
                        if (observerMethod != null) {
                            info = String.format("method %s.%s()", observerMethod.declaringClass().name(),
                                    observerMethod.name());
                        } else {
                            info = beanClass.toString();
                        }
                        LOGGER.debugf("Observer %s vetoed by %s", info, transformer.getClass().getName());
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

        if (!transactionPhase.equals(TransactionPhase.IN_PROGRESS) && !jtaCapabilities) {
            String info;
            if (observerMethod != null) {
                info = String.format("method %s.%s()", observerMethod.declaringClass().name(),
                        observerMethod.name());
            } else {
                info = beanClass.toString();
            }
            LOGGER.warnf(
                    "The observer %s makes use of %s transactional observers but no JTA capabilities were detected. Transactional observers will be notified at the same time as other observers.",
                    info, transactionPhase);
        }
        return new ObserverInfo(id, beanDeployment, beanClass, declaringBean, observerMethod, injection, eventParameter,
                isAsync, priority, reception, transactionPhase, observedType, qualifiers, notify);
    }

    private final String id;

    private final BeanDeployment beanDeployment;

    private final DotName beanClass;

    private final BeanInfo declaringBean;

    private final MethodInfo observerMethod;

    private final Injection injection;

    private final MethodParameterInfo eventParameter;

    private final int eventMetadataParameterPosition;

    private final int priority;

    private final boolean isAsync;

    private final Reception reception;

    private final TransactionPhase transactionPhase;

    private final Type observedType;

    private final Set<AnnotationInstance> qualifiers;

    // Following fields are only used by synthetic observers

    private final Consumer<MethodCreator> notify;

    ObserverInfo(String id, BeanDeployment beanDeployment, DotName beanClass, BeanInfo declaringBean, MethodInfo observerMethod,
            Injection injection,
            MethodParameterInfo eventParameter,
            boolean isAsync, int priority, Reception reception, TransactionPhase transactionPhase,
            Type observedType, Set<AnnotationInstance> qualifiers, Consumer<MethodCreator> notify) {
        this.id = id;
        this.beanDeployment = beanDeployment;
        this.beanClass = beanClass;
        this.declaringBean = declaringBean;
        this.observerMethod = observerMethod;
        this.injection = injection;
        this.eventParameter = eventParameter;
        this.eventMetadataParameterPosition = initEventMetadataParam(observerMethod);
        this.isAsync = isAsync;
        this.priority = priority;
        this.reception = reception;
        this.transactionPhase = transactionPhase;
        this.observedType = observedType;
        this.qualifiers = qualifiers;
        this.notify = notify;
    }

    @Override
    public TargetKind kind() {
        return TargetKind.OBSERVER;
    }

    @Override
    public ObserverInfo asObserver() {
        return this;
    }

    /**
     * A unique identifier should be used for multiple synthetic observer methods with the same
     * attributes (including the bean class).
     * 
     * @return the optional identifier
     */
    public String getId() {
        return id;
    }

    BeanDeployment getBeanDeployment() {
        return beanDeployment;
    }

    /**
     * 
     * @return the class of the declaring bean or the class provided by the configurator for synthetic observers
     */
    public DotName getBeanClass() {
        return beanClass;
    }

    /**
     * 
     * @return the declaring bean or null in case of synthetic observer
     */
    public BeanInfo getDeclaringBean() {
        return declaringBean;
    }

    public boolean isSynthetic() {
        return getDeclaringBean() == null;
    }

    /**
     * 
     * @return the observer method or null in case of synthetic observer
     */
    public MethodInfo getObserverMethod() {
        return observerMethod;
    }

    /**
     * 
     * @return the event parameter or null in case of synthetic observer
     */
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
        return observedType;
    }

    public Set<AnnotationInstance> getQualifiers() {
        return qualifiers;
    }

    Consumer<MethodCreator> getNotify() {
        return notify;
    }

    void init(List<Throwable> errors) {
        if (injection != null) {
            for (InjectionPointInfo injectionPoint : injection.injectionPoints) {
                Beans.resolveInjectionPoint(declaringBean.getDeployment(), this, injectionPoint, errors);
            }
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
        for (AnnotationInstance annotation : getParameterAnnotations(beanDeployment, observerMethod,
                eventParameter.position())) {
            beanDeployment.extractQualifiers(annotation).forEach(qualifiers::add);
        }
        return qualifiers;
    }

    public int getPriority() {
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
        if (observerMethod != null) {
            for (ListIterator<Type> iterator = observerMethod.parameters().listIterator(); iterator.hasNext();) {
                if (iterator.next().name().equals(DotNames.EVENT_METADATA)) {
                    return iterator.previousIndex();
                }
            }
        }
        return -1;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ObserverInfo [beanClass=").append(beanClass).append(", priority=").append(priority).append(", isAsync=")
                .append(isAsync).append(", reception=").append(reception).append(", transactionPhase=").append(transactionPhase)
                .append(", observedType=").append(observedType).append(", qualifiers=").append(qualifiers).append("]");
        return builder.toString();
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
            return target != null ? target.asMethod() : null;
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
