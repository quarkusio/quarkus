package io.quarkus.arc.deployment;

import java.util.function.Predicate;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.ClientProxy;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.deployment.ObserverRegistrationPhaseBuildItem.ObserverConfiguratorBuildItem;
import io.quarkus.arc.impl.CreationalContextImpl;
import io.quarkus.arc.processor.AnnotationStore;
import io.quarkus.arc.processor.Annotations;
import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.arc.processor.BuildExtension;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.arc.processor.ObserverConfigurator;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.runtime.Startup;
import io.quarkus.runtime.StartupEvent;

public class StartupBuildSteps {

    static final DotName STARTUP_NAME = DotName.createSimple(Startup.class.getName());

    static final MethodDescriptor ARC_CONTAINER = MethodDescriptor.ofMethod(Arc.class, "container", ArcContainer.class);
    static final MethodDescriptor ARC_CONTAINER_BEAN = MethodDescriptor.ofMethod(ArcContainer.class, "bean",
            InjectableBean.class, String.class);
    static final MethodDescriptor ARC_CONTAINER_INSTANCE = MethodDescriptor.ofMethod(ArcContainer.class, "instance",
            InstanceHandle.class, InjectableBean.class);
    static final MethodDescriptor INSTANCE_HANDLE_GET = MethodDescriptor.ofMethod(InstanceHandle.class, "get", Object.class);
    static final MethodDescriptor CLIENT_PROXY_CONTEXTUAL_INSTANCE = MethodDescriptor.ofMethod(ClientProxy.class,
            "arc_contextualInstance", Object.class);
    static final MethodDescriptor CONTEXTUAL_CREATE = MethodDescriptor.ofMethod(Contextual.class,
            "create", Object.class, CreationalContext.class);
    static final MethodDescriptor CONTEXTUAL_DESTROY = MethodDescriptor.ofMethod(Contextual.class,
            "destroy", void.class, Object.class, CreationalContext.class);

    private static final Logger LOGGER = Logger.getLogger(StartupBuildSteps.class);

    @BuildStep
    AnnotationsTransformerBuildItem annotationTransformer() {
        return new AnnotationsTransformerBuildItem(new AnnotationsTransformer() {

            @Override
            public boolean appliesTo(org.jboss.jandex.AnnotationTarget.Kind kind) {
                return kind == org.jboss.jandex.AnnotationTarget.Kind.CLASS;
            }

            @Override
            public void transform(TransformationContext context) {
                if (context.isClass() && !BuiltinScope.isDeclaredOn(context.getTarget().asClass())) {
                    // Class with no built-in scope annotation but with @Scheduled method
                    if (Annotations.contains(context.getTarget().asClass().classAnnotations(), STARTUP_NAME)) {
                        LOGGER.debugf("Found @Startup on a class %s with no scope annotations - adding @ApplicationScoped",
                                context.getTarget());
                        context.transform().add(ApplicationScoped.class).done();
                    }
                }
            }
        });
    }

    @BuildStep
    UnremovableBeanBuildItem unremovableBeans() {
        // Make all classes annotated with @Startup unremovable
        return new UnremovableBeanBuildItem(new Predicate<BeanInfo>() {
            @Override
            public boolean test(BeanInfo bean) {
                if (bean.isClassBean()) {
                    return bean.getTarget().get().asClass().annotations().containsKey(STARTUP_NAME);
                } else if (bean.isProducerMethod()) {
                    return bean.getTarget().get().asMethod().hasAnnotation(STARTUP_NAME);
                } else if (bean.isProducerField()) {
                    return bean.getTarget().get().asField().hasAnnotation(STARTUP_NAME);
                }
                return false;
            }
        });
    }

    @BuildStep
    void registerStartupObservers(ObserverRegistrationPhaseBuildItem observerRegistrationPhase,
            BuildProducer<ObserverConfiguratorBuildItem> configurators) {

        AnnotationStore annotationStore = observerRegistrationPhase.getContext().get(BuildExtension.Key.ANNOTATION_STORE);

        for (BeanInfo bean : observerRegistrationPhase.getContext().beans().withTarget()) {
            AnnotationInstance startupAnnotation = annotationStore.getAnnotation(bean.getTarget().get(), STARTUP_NAME);
            if (startupAnnotation != null) {
                registerStartupObserver(observerRegistrationPhase, bean, startupAnnotation);
            }
        }
    }

    private void registerStartupObserver(ObserverRegistrationPhaseBuildItem observerRegistrationPhase, BeanInfo bean,
            AnnotationInstance startup) {
        ObserverConfigurator configurator = observerRegistrationPhase.getContext().configure()
                .beanClass(bean.getBeanClass())
                .observedType(StartupEvent.class);
        AnnotationValue priority = startup.value();
        if (priority != null) {
            configurator.priority(priority.asInt());
        }
        configurator.notify(mc -> {
            // InjectableBean<Foo> bean = Arc.container().bean("bflmpsvz");
            ResultHandle containerHandle = mc.invokeStaticMethod(ARC_CONTAINER);
            ResultHandle beanHandle = mc.invokeInterfaceMethod(ARC_CONTAINER_BEAN, containerHandle,
                    mc.load(bean.getIdentifier()));
            if (BuiltinScope.DEPENDENT.is(bean.getScope())) {
                // It does not make a lot of sense to support @Startup dependent beans but it's still a valid use case
                ResultHandle contextHandle = mc.newInstance(
                        MethodDescriptor.ofConstructor(CreationalContextImpl.class, Contextual.class),
                        beanHandle);
                // Create a dependent instance
                ResultHandle instanceHandle = mc.invokeInterfaceMethod(CONTEXTUAL_CREATE, beanHandle,
                        contextHandle);
                // But destroy the instance immediately
                mc.invokeInterfaceMethod(CONTEXTUAL_DESTROY, beanHandle, instanceHandle, contextHandle);
            } else {
                // Obtains the instance from the context
                // InstanceHandle<Foo> handle = Arc.container().instance(bean);
                ResultHandle instanceHandle = mc.invokeInterfaceMethod(ARC_CONTAINER_INSTANCE, containerHandle,
                        beanHandle);
                if (bean.getScope().isNormal()) {
                    // We need to unwrap the client proxy
                    // ((ClientProxy) handle.get()).arc_contextualInstance();
                    ResultHandle proxyHandle = mc.checkCast(
                            mc.invokeInterfaceMethod(INSTANCE_HANDLE_GET, instanceHandle), ClientProxy.class);
                    mc.invokeInterfaceMethod(CLIENT_PROXY_CONTEXTUAL_INSTANCE, proxyHandle);
                }
            }
            mc.returnValue(null);
        });
        configurator.done();
    }
}
