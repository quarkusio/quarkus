package io.quarkus.arc.deployment;

import static io.quarkus.arc.processor.Annotations.getAnnotations;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.OptionalInt;
import java.util.function.Predicate;

import jakarta.enterprise.context.spi.Contextual;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.ObserverMethod;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.ClientProxy;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.deployment.AutoAddScopeBuildItem.MatchPredicate;
import io.quarkus.arc.deployment.ObserverRegistrationPhaseBuildItem.ObserverConfiguratorBuildItem;
import io.quarkus.arc.impl.CreationalContextImpl;
import io.quarkus.arc.processor.AnnotationStore;
import io.quarkus.arc.processor.Annotations;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.arc.processor.BuildExtension;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.arc.processor.InjectionPointInfo;
import io.quarkus.arc.processor.ObserverConfigurator;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.gizmo.CatchBlockCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.gizmo.TryBlock;
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

    private static final Logger LOG = Logger.getLogger(StartupBuildSteps.class);

    @BuildStep
    AutoAddScopeBuildItem addScope(CustomScopeAnnotationsBuildItem customScopes) {
        // Class with no built-in scope annotation but with @Startup annotation
        return AutoAddScopeBuildItem.builder()
                .defaultScope(BuiltinScope.APPLICATION)
                .match(new MatchPredicate() {
                    @Override
                    public boolean test(ClassInfo clazz, Collection<AnnotationInstance> annotations, IndexView index) {
                        if (Annotations.contains(annotations, STARTUP_NAME)) {
                            // Class annotated with @Startup
                            return true;
                        }
                        for (MethodInfo method : clazz.methods()) {
                            if (method.hasAnnotation(STARTUP_NAME)
                                    && !method.hasAnnotation(DotNames.PRODUCES)) {
                                // @Startup methods but not producers
                                return true;
                            }
                        }
                        return false;
                    }
                })
                .reason("Found classes containing @Startup annotation.")
                .build();
    }

    @BuildStep
    UnremovableBeanBuildItem unremovableBeans() {
        return new UnremovableBeanBuildItem(new Predicate<BeanInfo>() {
            @Override
            public boolean test(BeanInfo bean) {
                if (bean.isClassBean()) {
                    return bean.getTarget().get().asClass().annotationsMap().containsKey(STARTUP_NAME);
                } else if (bean.isProducerMethod()) {
                    return !getAnnotations(Kind.METHOD, STARTUP_NAME, bean.getTarget().get().asMethod().annotations())
                            .isEmpty();
                } else if (bean.isProducerField()) {
                    return bean.getTarget().get().asField().hasAnnotation(STARTUP_NAME);
                }
                // No target - synthetic bean
                return false;
            }
        });
    }

    @BuildStep
    void registerStartupObservers(ObserverRegistrationPhaseBuildItem observerRegistration,
            BuildProducer<ObserverConfiguratorBuildItem> configurators) {

        AnnotationStore annotationStore = observerRegistration.getContext().get(BuildExtension.Key.ANNOTATION_STORE);

        for (BeanInfo bean : observerRegistration.getContext().beans()) {
            if (bean.isSynthetic()) {
                OptionalInt startupPriority = bean.getStartupPriority();
                if (startupPriority.isPresent()) {
                    registerStartupObserver(observerRegistration, bean, bean.getIdentifier(),
                            startupPriority.getAsInt(), null);
                }
            } else {
                // First check if the target is annotated with @Startup
                // Class for class-based bean, method for producer method, etc.
                AnnotationTarget target = bean.getTarget().get();
                AnnotationInstance startupAnnotation = annotationStore.getAnnotation(target, STARTUP_NAME);
                if (startupAnnotation != null) {
                    AnnotationValue priority = startupAnnotation.value();
                    registerStartupObserver(observerRegistration, bean, bean.getIdentifier(),
                            priority != null ? priority.asInt() : ObserverMethod.DEFAULT_PRIORITY, null);
                }
                if (target.kind() == Kind.CLASS) {
                    // If the target is a class then collect all non-static non-producer no-args methods annotated with @Startup
                    List<MethodInfo> startupMethods = new ArrayList<>();
                    for (MethodInfo method : target.asClass().methods()) {
                        if (annotationStore.hasAnnotation(method, STARTUP_NAME)) {
                            if (!method.isSynthetic()
                                    && !Modifier.isPrivate(method.flags())
                                    && !Modifier.isStatic(method.flags())
                                    && method.parametersCount() == 0
                                    && !annotationStore.hasAnnotation(method, DotNames.PRODUCES)) {
                                startupMethods.add(method);
                            } else {
                                if (!annotationStore.hasAnnotation(method, DotNames.PRODUCES)) {
                                    // Producer methods annotated with @Startup are valid and processed above
                                    LOG.warnf("Ignored an invalid @Startup method declared on %s: %s",
                                            method.declaringClass().name(),
                                            method);
                                }
                            }
                        }
                    }
                    if (!startupMethods.isEmpty()) {
                        for (MethodInfo method : startupMethods) {
                            AnnotationValue priority = annotationStore.getAnnotation(method, STARTUP_NAME).value();
                            registerStartupObserver(observerRegistration, bean, bean.getIdentifier() + method.toString(),
                                    priority != null ? priority.asInt() : ObserverMethod.DEFAULT_PRIORITY, method);
                        }
                    }
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

            // if the [synthetic] bean is not active and is not injected in an always-active bean, skip obtaining the instance
            // this means that an inactive bean that is injected into an always-active bean will end up with an error
            if (bean.canBeInactive()) {
                boolean isInjectedInAlwaysActiveBean = false;
                for (InjectionPointInfo ip : observerRegistration.getBeanProcessor().getBeanDeployment().getInjectionPoints()) {
                    if (bean.equals(ip.getResolvedBean()) && ip.getTargetBean().isPresent()
                            && !ip.getTargetBean().get().canBeInactive()) {
                        isInjectedInAlwaysActiveBean = true;
                        break;
                    }
                }

                if (!isInjectedInAlwaysActiveBean) {
                    ResultHandle isActive = mc.invokeInterfaceMethod(
                            MethodDescriptor.ofMethod(InjectableBean.class, "isActive", boolean.class),
                            beanHandle);
                    mc.ifFalse(isActive).trueBranch().returnVoid();
                }
            }

            if (BuiltinScope.DEPENDENT.is(bean.getScope())) {
                // It does not make a lot of sense to support @Startup dependent beans but it's still a valid use case
                ResultHandle creationalContext = mc.newInstance(
                        MethodDescriptor.ofConstructor(CreationalContextImpl.class, Contextual.class),
                        beanHandle);
                // Create a dependent instance
                ResultHandle instance = mc.invokeInterfaceMethod(CONTEXTUAL_CREATE, beanHandle,
                        creationalContext);
                if (startupMethod != null) {
                    TryBlock tryBlock = mc.tryBlock();
                    tryBlock.invokeVirtualMethod(MethodDescriptor.of(startupMethod), instance);
                    CatchBlockCreator catchBlock = tryBlock.addCatch(Exception.class);
                    catchBlock.invokeInterfaceMethod(CONTEXTUAL_DESTROY, beanHandle, instance, creationalContext);
                    catchBlock.throwException(RuntimeException.class, "Error destroying bean with @Startup method",
                            catchBlock.getCaughtException());
                }
                // Destroy the instance immediately
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
            mc.returnVoid();
        });
        configurator.done();
    }
}
