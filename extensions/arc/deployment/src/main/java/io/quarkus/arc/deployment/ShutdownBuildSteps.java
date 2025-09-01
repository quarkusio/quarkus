package io.quarkus.arc.deployment;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import jakarta.enterprise.context.spi.Contextual;
import jakarta.enterprise.inject.spi.ObserverMethod;

import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.ObserverRegistrationPhaseBuildItem.ObserverConfiguratorBuildItem;
import io.quarkus.arc.impl.CreationalContextImpl;
import io.quarkus.arc.processor.AnnotationStore;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.arc.processor.BuildExtension;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.arc.processor.ObserverConfigurator;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.shutdown.ShutdownBuildTimeConfig;
import io.quarkus.gizmo.CatchBlockCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.gizmo.TryBlock;
import io.quarkus.runtime.Shutdown;
import io.quarkus.runtime.ShutdownDelayInitiated;
import io.quarkus.runtime.ShutdownDelayInitiatedEvent;
import io.quarkus.runtime.ShutdownEvent;

public class ShutdownBuildSteps {

    static final DotName SHUTDOWN_NAME = DotName.createSimple(Shutdown.class.getName());
    static final DotName SHUTDOWN_DELAY_NAME = DotName.createSimple(ShutdownDelayInitiated.class.getName());

    private static final Logger LOG = Logger.getLogger(ShutdownBuildSteps.class);

    @BuildStep
    AutoAddScopeBuildItem shutdownAddScope(CustomScopeAnnotationsBuildItem customScopes, ShutdownBuildTimeConfig config) {
        // Class with no built-in scope annotation but with @Shutdown/@ShutdownDelayInitiated annotation
        return AutoAddScopeBuildItem.builder()
                .defaultScope(BuiltinScope.APPLICATION)
                .anyMethodMatches(new Predicate<MethodInfo>() {
                    @Override
                    public boolean test(MethodInfo m) {
                        return m.hasAnnotation(SHUTDOWN_NAME)
                                || (config.delayEnabled() && m.hasAnnotation(SHUTDOWN_DELAY_NAME));
                    }
                })
                .reason("Found classes containing @Shutdown/@ShutdownDelayInitiated annotation..")
                .build();
    }

    @BuildStep
    UnremovableBeanBuildItem unremovableBeans(ShutdownBuildTimeConfig config) {
        return new UnremovableBeanBuildItem(new Predicate<BeanInfo>() {
            @Override
            public boolean test(BeanInfo bean) {
                if (bean.isClassBean()) {
                    ClassInfo clasInfo = bean.getTarget().get().asClass();
                    return clasInfo.hasAnnotation(SHUTDOWN_NAME)
                            || (config.delayEnabled() && clasInfo.hasAnnotation(SHUTDOWN_DELAY_NAME));
                }
                return false;
            }
        });
    }

    @BuildStep
    void registerShutdownObservers(ObserverRegistrationPhaseBuildItem observerRegistration,
            ShutdownBuildTimeConfig shutdownConfig,
            BuildProducer<ObserverConfiguratorBuildItem> configurators) {

        AnnotationStore annotationStore = observerRegistration.getContext().get(BuildExtension.Key.ANNOTATION_STORE);

        for (BeanInfo bean : observerRegistration.getContext().beans().classBeans()) {
            ClassInfo beanClass = bean.getTarget().get().asClass();
            List<MethodInfo> shutdownMethods = new ArrayList<>();
            List<MethodInfo> shutdownDelayedMethods = new ArrayList<>();
            // Collect all non-static no-args methods annotated with @Shutdown or @ShutdownDelayInitiated
            for (MethodInfo method : beanClass.methods()) {
                if (annotationStore.hasAnnotation(method, SHUTDOWN_NAME)) {
                    validateMethod(method, shutdownMethods, SHUTDOWN_NAME.withoutPackagePrefix());
                }
                if (annotationStore.hasAnnotation(method, SHUTDOWN_DELAY_NAME)) {
                    validateMethod(method, shutdownDelayedMethods, SHUTDOWN_DELAY_NAME.withoutPackagePrefix());
                }
            }
            if (!shutdownMethods.isEmpty()) {
                processMethods(shutdownMethods, annotationStore, SHUTDOWN_NAME, observerRegistration, bean);
            }
            // only process these methods if the shutdown delay config is enabled
            if (!shutdownDelayedMethods.isEmpty() && shutdownConfig.delayEnabled()) {
                processMethods(shutdownDelayedMethods, annotationStore, SHUTDOWN_DELAY_NAME, observerRegistration, bean);
            }
        }
    }

    private void validateMethod(MethodInfo method, List<MethodInfo> validMethods, String annotationName) {
        if (!method.isSynthetic()
                && !Modifier.isPrivate(method.flags())
                && !Modifier.isStatic(method.flags())
                && method.parametersCount() == 0) {
            validMethods.add(method);
        } else {
            LOG.warnf("Ignored an invalid @" + annotationName + " method declared on %s: %s", method.declaringClass().name(),
                    method);
        }
    }

    private void processMethods(List<MethodInfo> shutdownMethods, AnnotationStore annotationStore, DotName annotationName,
            ObserverRegistrationPhaseBuildItem observerRegistration, BeanInfo bean) {
        for (MethodInfo method : shutdownMethods) {
            AnnotationValue priority = annotationStore.getAnnotation(method, annotationName).value();
            registerShutdownObserver(observerRegistration, bean,
                    method.declaringClass().name() + "#" + method.toString(),
                    priority != null ? priority.asInt() : ObserverMethod.DEFAULT_PRIORITY, method, annotationName);
        }
    }

    private void registerShutdownObserver(ObserverRegistrationPhaseBuildItem observerRegistration, BeanInfo bean, String id,
            int priority, MethodInfo shutdownMethod, DotName observedType) {
        ObserverConfigurator configurator = observerRegistration.getContext().configure()
                .beanClass(bean.getBeanClass())
                .observedType(observedType.equals(SHUTDOWN_NAME) ? ShutdownEvent.class : ShutdownDelayInitiatedEvent.class);
        configurator.id(id);
        configurator.priority(priority);
        configurator.notify(mc -> {
            // InjectableBean<Foo> bean = Arc.container().bean("bflmpsvz");
            ResultHandle containerHandle = mc.invokeStaticMethod(StartupBuildSteps.ARC_CONTAINER);
            ResultHandle beanHandle = mc.invokeInterfaceMethod(StartupBuildSteps.ARC_CONTAINER_BEAN, containerHandle,
                    mc.load(bean.getIdentifier()));
            if (BuiltinScope.DEPENDENT.is(bean.getScope())) {
                ResultHandle creationalContext = mc.newInstance(
                        MethodDescriptor.ofConstructor(CreationalContextImpl.class, Contextual.class),
                        beanHandle);
                // Create a dependent instance
                ResultHandle instance = mc.invokeInterfaceMethod(StartupBuildSteps.CONTEXTUAL_CREATE, beanHandle,
                        creationalContext);
                TryBlock tryBlock = mc.tryBlock();
                tryBlock.invokeVirtualMethod(MethodDescriptor.of(shutdownMethod), instance);
                CatchBlockCreator catchBlock = tryBlock.addCatch(Exception.class);
                catchBlock.invokeInterfaceMethod(StartupBuildSteps.CONTEXTUAL_DESTROY, beanHandle, instance, creationalContext);
                catchBlock.throwException(RuntimeException.class, "Error destroying bean with @Shutdown method",
                        catchBlock.getCaughtException());
                // Destroy the instance immediately
                mc.invokeInterfaceMethod(StartupBuildSteps.CONTEXTUAL_DESTROY, beanHandle, instance, creationalContext);
            } else {
                // Obtains the instance from the context
                // InstanceHandle<Foo> handle = Arc.container().instance(bean);
                ResultHandle instanceHandle = mc.invokeInterfaceMethod(StartupBuildSteps.ARC_CONTAINER_INSTANCE,
                        containerHandle,
                        beanHandle);
                ResultHandle instance = mc.invokeInterfaceMethod(StartupBuildSteps.INSTANCE_HANDLE_GET, instanceHandle);
                mc.invokeVirtualMethod(MethodDescriptor.of(shutdownMethod), instance);
            }
            mc.returnValue(null);
        });
        configurator.done();
    }
}
