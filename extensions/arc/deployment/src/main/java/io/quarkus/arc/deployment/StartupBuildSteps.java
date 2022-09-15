package io.quarkus.arc.deployment;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.enterprise.context.spi.Contextual;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.ObserverMethod;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.ClientProxy;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.deployment.ObserverRegistrationPhaseBuildItem.ObserverConfiguratorBuildItem;
import io.quarkus.arc.impl.CreationalContextImpl;
import io.quarkus.arc.processor.AnnotationStore;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.arc.processor.BuildExtension;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.arc.processor.DotNames;
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

    @BuildStep
    AutoAddScopeBuildItem addScope(CustomScopeAnnotationsBuildItem customScopes) {
        // Class with no built-in scope annotation but with @Startup method
        return AutoAddScopeBuildItem.builder()
                .defaultScope(BuiltinScope.APPLICATION)
                .isAnnotatedWith(STARTUP_NAME)
                .reason("Found classes containing @Startup annotation.")
                .build();
    }

    @BuildStep
    UnremovableBeanBuildItem unremovableBeans() {
        // Make all classes annotated with @Startup unremovable
        return UnremovableBeanBuildItem.targetWithAnnotation(STARTUP_NAME);
    }

    @BuildStep
    void registerStartupObservers(ObserverRegistrationPhaseBuildItem observerRegistration,
            BuildProducer<ObserverConfiguratorBuildItem> configurators) {

        AnnotationStore annotationStore = observerRegistration.getContext().get(BuildExtension.Key.ANNOTATION_STORE);

        for (BeanInfo bean : observerRegistration.getContext().beans().withTarget()) {
            // First check if the target is annotated with @Startup
            // Class for class-based bean, method for producer method, etc.
            AnnotationTarget target = bean.getTarget().get();
            AnnotationInstance startupAnnotation = annotationStore.getAnnotation(target, STARTUP_NAME);
            if (startupAnnotation != null) {
                String id;
                if (target.kind() == Kind.METHOD) {
                    id = target.asMethod().declaringClass().name() + "#" + target.asMethod().toString();
                } else if (target.kind() == Kind.FIELD) {
                    id = target.asField().declaringClass().name() + "#" + target.asField().toString();
                } else {
                    id = target.asClass().name().toString();
                }
                AnnotationValue priority = startupAnnotation.value();
                registerStartupObserver(observerRegistration, bean, id,
                        priority != null ? priority.asInt() : ObserverMethod.DEFAULT_PRIORITY, null);
            }

            List<MethodInfo> startupMethods = Collections.emptyList();
            if (target.kind() == Kind.CLASS) {
                // If the target is a class then collect all non-static non-producer no-args methods annotated with @Startup
                startupMethods = new ArrayList<>();
                for (MethodInfo method : target.asClass().methods()) {
                    if (!method.isSynthetic()
                            && !Modifier.isStatic(method.flags())
                            && method.parametersCount() == 0
                            && annotationStore.hasAnnotation(method, STARTUP_NAME)
                            && !annotationStore.hasAnnotation(method, DotNames.PRODUCES)) {
                        startupMethods.add(method);
                    }
                }
            }
            if (!startupMethods.isEmpty()) {
                for (MethodInfo method : startupMethods) {
                    AnnotationValue priority = annotationStore.getAnnotation(method, STARTUP_NAME).value();
                    registerStartupObserver(observerRegistration, bean,
                            method.declaringClass().name() + "#" + method.toString(),
                            priority != null ? priority.asInt() : ObserverMethod.DEFAULT_PRIORITY, method);
                }
            }
        }
    }

    private void registerStartupObserver(ObserverRegistrationPhaseBuildItem observerRegistration, BeanInfo bean, String id,
            int priority, MethodInfo startupMethod) {
        ObserverConfigurator configurator = observerRegistration.getContext().configure()
                .beanClass(bean.getBeanClass())
                .observedType(StartupEvent.class);
        configurator.id(id);
        configurator.priority(priority);
        configurator.notify(mc -> {
            // InjectableBean<Foo> bean = Arc.container().bean("bflmpsvz");
            ResultHandle containerHandle = mc.invokeStaticMethod(ARC_CONTAINER);
            ResultHandle beanHandle = mc.invokeInterfaceMethod(ARC_CONTAINER_BEAN, containerHandle,
                    mc.load(bean.getIdentifier()));
            if (BuiltinScope.DEPENDENT.is(bean.getScope())) {
                // It does not make a lot of sense to support @Startup dependent beans but it's still a valid use case
                ResultHandle creationalContext = mc.newInstance(
                        MethodDescriptor.ofConstructor(CreationalContextImpl.class, Contextual.class),
                        beanHandle);
                // Create a dependent instance
                ResultHandle instance = mc.invokeInterfaceMethod(CONTEXTUAL_CREATE, beanHandle,
                        creationalContext);
                if (startupMethod != null) {
                    mc.invokeVirtualMethod(MethodDescriptor.of(startupMethod), instance);
                }
                // But destroy the instance immediately
                mc.invokeInterfaceMethod(CONTEXTUAL_DESTROY, beanHandle, instance, creationalContext);
            } else {
                // Obtains the instance from the context
                // InstanceHandle<Foo> handle = Arc.container().instance(bean);
                ResultHandle instanceHandle = mc.invokeInterfaceMethod(ARC_CONTAINER_INSTANCE, containerHandle,
                        beanHandle);
                ResultHandle instance = mc.invokeInterfaceMethod(INSTANCE_HANDLE_GET, instanceHandle);
                if (startupMethod != null) {
                    mc.invokeVirtualMethod(MethodDescriptor.of(startupMethod), instance);
                } else if (bean.getScope().isNormal()) {
                    // We need to unwrap the client proxy
                    // ((ClientProxy) handle.get()).arc_contextualInstance();
                    ResultHandle proxyHandle = mc.checkCast(instance, ClientProxy.class);
                    mc.invokeInterfaceMethod(CLIENT_PROXY_CONTEXTUAL_INSTANCE, proxyHandle);
                }
            }
            mc.returnValue(null);
        });
        configurator.done();
    }
}
