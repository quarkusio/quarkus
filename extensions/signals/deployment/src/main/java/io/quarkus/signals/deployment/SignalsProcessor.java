package io.quarkus.signals.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;
import static io.quarkus.signals.deployment.ReceiverExecutorImplementationBuildItem.ReceiverExecutorImplementation.DEFAULT_BLOCKING;
import static io.quarkus.signals.deployment.ReceiverExecutorImplementationBuildItem.ReceiverExecutorImplementation.VERTX;

import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.invoke.Invoker;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.MethodParameterInfo;
import org.jboss.jandex.MethodSignatureKey;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.PrimitiveType;
import org.jboss.jandex.Type.Kind;
import org.jboss.jandex.TypeVariable;
import org.jboss.jandex.gizmo2.Jandex2Gizmo;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AutoAddScopeBuildItem;
import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.arc.deployment.BeanDiscoveryFinishedBuildItem;
import io.quarkus.arc.deployment.BeanRegistrationPhaseBuildItem;
import io.quarkus.arc.deployment.InvokerFactoryBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem.ExtendedBeanConfigurator;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.arc.processor.InjectionPointInfo;
import io.quarkus.arc.processor.InvokerBuilder;
import io.quarkus.arc.processor.RuntimeTypeCreator;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.GeneratedClassGizmo2Adaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.gizmo2.ClassOutput;
import io.quarkus.gizmo2.Const;
import io.quarkus.gizmo2.Expr;
import io.quarkus.gizmo2.GenericType;
import io.quarkus.gizmo2.Gizmo;
import io.quarkus.gizmo2.LocalVar;
import io.quarkus.gizmo2.TypeArgument;
import io.quarkus.gizmo2.desc.ConstructorDesc;
import io.quarkus.gizmo2.desc.FieldDesc;
import io.quarkus.gizmo2.desc.MethodDesc;
import io.quarkus.runtime.util.HashUtil;
import io.quarkus.signals.Signal;
import io.quarkus.signals.runtime.impl.DefaultBlockingReceiverExecutor;
import io.quarkus.signals.runtime.impl.DefaultRequestContextInterceptor;
import io.quarkus.signals.runtime.impl.InvokerReceiver;
import io.quarkus.signals.runtime.impl.InvokerReceiver.ReceiverInfo;
import io.quarkus.signals.runtime.impl.ReceiverManager;
import io.quarkus.signals.runtime.impl.SignalBeanCreator;
import io.quarkus.signals.runtime.impl.SignalsRecorder;
import io.quarkus.signals.runtime.impl.SignalsRecorder.SignalsContext;
import io.quarkus.signals.runtime.impl.VertxReceiverExecutor;
import io.quarkus.signals.runtime.impl.VertxRequestContextInterceptor;
import io.quarkus.signals.spi.Receiver.ExecutionModel;

class SignalsProcessor {

    @BuildStep
    void collectReceivers(BeanRegistrationPhaseBuildItem beanRegistration,
            BeanArchiveIndexBuildItem beanArchiveIndex,
            InvokerFactoryBuildItem invokerFactory,
            BuildProducer<ReceiverMethodBuildItem> receivers,
            ReceiverExecutorImplementationBuildItem receiverExecutorImplementation) {

        IndexView index = beanArchiveIndex.getIndex();

        // Collect all classes that directly declare @Receives methods
        Set<DotName> classesWithReceivers = new HashSet<>();
        for (AnnotationInstance annotation : index.getAnnotations(DotNames.RECEIVES)) {
            if (annotation.target().kind() == AnnotationTarget.Kind.METHOD_PARAMETER) {
                classesWithReceivers.add(annotation.target().asMethodParameter().method().declaringClass().name());
            }
        }

        if (classesWithReceivers.isEmpty()) {
            // No @Receives methods found
            return;
        }

        Set<DotName> knownQualifiers = beanRegistration.getBeanProcessor().getBeanDeployment().getQualifiers()
                .stream()
                .map(ClassInfo::name)
                .collect(Collectors.toSet());

        for (BeanInfo bean : beanRegistration.getContext().beans().classBeans()) {
            ClassInfo beanClass = bean.getTarget().get().asClass();
            if (!hasReceiverInHierarchy(beanClass, classesWithReceivers, index)) {
                continue;
            }
            // Walk the class hierarchy; methods seen in subclasses override those from superclasses
            Set<MethodSignatureKey> seenMethods = new HashSet<>();
            ClassInfo current = beanClass;
            while (current != null) {
                for (MethodInfo method : current.methods()) {
                    if (method.isSynthetic() || method.isConstructor()) {
                        continue;
                    }
                    List<MethodParameterInfo> params = method.parameters();
                    MethodParameterInfo signalParam = null;
                    if (!params.isEmpty()) {
                        for (MethodParameterInfo param : params) {
                            if (param.hasDeclaredAnnotation(DotNames.RECEIVES)) {
                                if (signalParam != null) {
                                    throw new IllegalStateException(
                                            "A receiver method must have exactly one parameter annotated with @Receives: "
                                                    + methodDesc(method));
                                }
                                signalParam = param;
                            }
                        }
                    }
                    if (!seenMethods.add(method.signatureKey())) {
                        // Overridden by a subclass
                        continue;
                    }
                    if (signalParam != null) {
                        if (Modifier.isPrivate(method.flags())) {
                            throw new IllegalStateException(
                                    "A receiver method must not be private: " + methodDesc(method));
                        }
                        if (Modifier.isStatic(method.flags())) {
                            throw new IllegalStateException(
                                    "A receiver method must not be static: " + methodDesc(method));
                        }
                        ExecutionModel executionModel = getExecutionModel(method);
                        if (!receiverExecutorImplementation.isSupported(executionModel)) {
                            throw new IllegalStateException(
                                    "%s execution model is not supported: %s".formatted(executionModel, methodDesc(method)));
                        }
                        InvokerBuilder invokerBuilder = invokerFactory.createInvoker(bean, method)
                                .withInstanceLookup();
                        if (params.size() > 1) {
                            for (MethodParameterInfo param : params) {
                                if (param != signalParam) {
                                    invokerBuilder.withArgumentLookup(param.position());
                                }
                            }
                        }
                        List<AnnotationInstance> qualifiers = new ArrayList<>();
                        for (AnnotationInstance a : signalParam.declaredAnnotations()) {
                            if (knownQualifiers.contains(a.name())) {
                                qualifiers.add(a);
                            }
                        }
                        receivers.produce(
                                new ReceiverMethodBuildItem(executionModel, bean, invokerBuilder.build(), method, signalParam,
                                        qualifiers));
                    }
                }
                DotName superName = current.superName();
                current = superName != null ? index.getClassByName(superName) : null;
            }
        }
    }

    private static boolean hasReceiverInHierarchy(ClassInfo clazz, Set<DotName> classesWithReceivers, IndexView index) {
        ClassInfo current = clazz;
        while (current != null) {
            if (classesWithReceivers.contains(current.name())) {
                return true;
            }
            DotName superName = current.superName();
            current = superName != null ? index.getClassByName(superName) : null;
        }
        return false;
    }

    @Record(RUNTIME_INIT)
    @BuildStep
    void generateReceivers(SignalsRecorder recorder,
            BeanRegistrationPhaseBuildItem beanRegistration,
            BeanArchiveIndexBuildItem beanArchiveIndex,
            List<ReceiverMethodBuildItem> receivers,
            OrderedSpiComponentsBuildItem orderedSpiComponents,
            BuildProducer<GeneratedClassBuildItem> generatedClasses,
            BuildProducer<GeneratedResourceBuildItem> generatedResources,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {

        ClassOutput classOutput = new GeneratedClassGizmo2Adaptor(generatedClasses, generatedResources,
                new Function<String, String>() {
                    @Override
                    public String apply(String generatedClassName) {
                        return generatedClassName.substring(0, generatedClassName.indexOf('_'));
                    }
                });
        Gizmo gizmo = Gizmo.create(classOutput)
                .withDebugInfo(false)
                .withParameters(false);

        Set<AnnotationInstance> allQualifiers = new HashSet<>();
        List<String> receiverClasses = new ArrayList<String>();

        for (ReceiverMethodBuildItem receiver : receivers) {
            String receiverClassName = receiver.getBean().getBeanClass()
                    + "_"
                    + receiver.getMethod().name()
                    + "_"
                    + HashUtil.sha256(receiver.getMethod().toString());
            gizmo.class_(receiverClassName, cc -> {
                var signalParamType = receiver.getSignalParam().type();
                if (signalParamType.kind() == Kind.PRIMITIVE) {
                    signalParamType = PrimitiveType.box(signalParamType.asPrimitiveType());
                }
                var returnType = receiver.getMethod().returnType();
                if (returnType.kind() != Kind.VOID && returnType.kind() == Kind.PRIMITIVE) {
                    returnType = PrimitiveType.box(returnType.asPrimitiveType());
                }
                cc.extends_(
                        GenericType.ofClass(InvokerReceiver.class,
                                Jandex2Gizmo.typeArgumentOf(signalParamType),
                                returnType.kind() == Kind.VOID ? TypeArgument.of(Void.class)
                                        : Jandex2Gizmo.typeArgumentOf(returnType)));

                FieldDesc signalTypeField = cc.field("signalType", fc -> {
                    fc.private_();
                    fc.final_();
                    fc.setType(Type.class);
                });
                FieldDesc qualifiersField = cc.field("qualifiers", fc -> {
                    fc.private_();
                    fc.final_();
                    fc.setType(Set.class);
                });
                FieldDesc responseTypeField = cc.field("responseType", fc -> {
                    fc.private_();
                    fc.final_();
                    fc.setType(Type.class);
                });

                cc.constructor(con -> {
                    con.body(bc -> {
                        Expr invoker = bc.new_(receiver.getInvoker().getClassDesc());
                        Expr receiveInfo = bc.new_(ReceiverInfo.class,
                                Const.of(receiver.getSignalParam().position()),
                                Const.of(receiver.getSignalParam().type().name().equals(DotNames.SIGNAL_CONTEXT)),
                                Const.of((short) receiver.getMethod().parametersCount()));
                        bc.invokeSpecial(ConstructorDesc.of(InvokerReceiver.class, Invoker.class, ReceiverInfo.class),
                                cc.this_(), invoker, receiveInfo);

                        LocalVar tccl = bc.localVar("tccl", bc.invokeVirtual(
                                MethodDesc.of(Thread.class, "getContextClassLoader", ClassLoader.class), bc.currentThread()));
                        // Signal type
                        RuntimeTypeCreator rttc = RuntimeTypeCreator.of(bc).withTCCL(tccl);
                        bc.set(cc.this_().field(signalTypeField), rttc.create(receiver.getSignalType()));
                        // Qualifiers
                        Expr qualifiersSet = bc.setOf(receiver.getQualifiers(),
                                qualifier -> beanRegistration.getBeanProcessor().getAnnotationLiteralProcessor().create(bc,
                                        beanArchiveIndex.getIndex().getClassByName(qualifier.name()), qualifier));
                        bc.set(cc.this_().field(qualifiersField), qualifiersSet);
                        // Response type
                        var responseType = receiver.getResponseType();
                        if (responseType != null) {
                            bc.set(cc.this_().field(responseTypeField), rttc.create(responseType));
                        } else {
                            bc.set(cc.this_().field(responseTypeField), Const.ofNull(Type.class));
                        }

                        bc.return_();
                    });
                });

                cc.method("executionModel", mc -> {
                    mc.returning(ExecutionModel.class);
                    mc.body(bc -> bc.return_(Const.of(receiver.getExecutionModel())));
                });

                cc.method("signalType", mc -> {
                    mc.returning(Type.class);
                    mc.body(bc -> bc.return_(cc.this_().field(signalTypeField)));
                });

                cc.method("qualifiers", mc -> {
                    mc.returning(Set.class);
                    mc.body(bc -> bc.return_(cc.this_().field(qualifiersField)));
                });

                cc.method("responseType", mc -> {
                    mc.returning(Type.class);
                    mc.body(bc -> bc.return_(cc.this_().field(responseTypeField)));
                });
            });
            receiverClasses.add(receiverClassName);
            allQualifiers.addAll(receiver.getQualifiers());
        }

        // Register public constructors for reflection
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(receiverClasses)
                .publicConstructors()
                .build());

        syntheticBeans.produce(SyntheticBeanBuildItem.configure(SignalsContext.class)
                .scope(Dependent.class)
                .setRuntimeInit()
                .supplier(recorder.createContext(receiverClasses,
                        orderedSpiComponents.getOrderedEnricherIds(),
                        orderedSpiComponents.getOrderedInterceptorIds()))
                .done());

        for (InjectionPointInfo ip : beanRegistration.getInjectionPoints()) {
            if (ip.getType().name().equals(DotNames.SIGNAL)) {
                allQualifiers.addAll(ip.getRequiredQualifiers());
            }
        }

        ExtendedBeanConfigurator signalConfigurator = SyntheticBeanBuildItem.configure(Signal.class)
                .addType(ParameterizedType.builder(DotNames.SIGNAL).addArgument(TypeVariable.create("T")).build())
                .scope(Dependent.class)
                .addInjectionPoint(ClassType.create(ReceiverManager.class))
                .addInjectionPoint(ClassType.create(InjectionPoint.class));
        for (AnnotationInstance q : allQualifiers) {
            signalConfigurator.addQualifier(q);
        }
        syntheticBeans.produce(signalConfigurator
                .creator(SignalBeanCreator.class)
                .forceApplicationClass()
                .done());
    }

    @BuildStep
    AutoAddScopeBuildItem addScopeToReceivers() {
        return AutoAddScopeBuildItem.builder()
                .containsAnnotations(DotNames.RECEIVES)
                .reason("Add @Singleton to a class with @Receives methods")
                .defaultScope(BuiltinScope.SINGLETON).build();
    }

    @BuildStep
    OrderedSpiComponentsBuildItem discoverSpiComponents(BeanDiscoveryFinishedBuildItem beanDiscovery) {
        List<String> orderedEnricherIds = discoverAndOrder(beanDiscovery, DotNames.SIGNAL_METADATA_ENRICHER,
                "SignalMetadataEnricher");
        List<String> orderedInterceptorIds = discoverAndOrder(beanDiscovery, DotNames.RECEIVER_INTERCEPTOR,
                "ReceiverInterceptor");
        return new OrderedSpiComponentsBuildItem(orderedEnricherIds, orderedInterceptorIds);
    }

    private static List<String> discoverAndOrder(BeanDiscoveryFinishedBuildItem beanDiscovery, DotName spiType,
            String componentTypeName) {
        Map<String, List<String>> beforeEdges = new HashMap<>();
        Map<String, List<String>> afterEdges = new HashMap<>();
        Set<String> allIds = new HashSet<>();

        for (BeanInfo bean : beanDiscovery.beanStream().withBeanType(spiType)) {
            String id = readIdentifier(bean, componentTypeName);
            if (!allIds.add(id)) {
                throw new IllegalStateException(
                        "Multiple " + componentTypeName + " beans with the same @Identifier value detected: " + id);
            }
            if (bean.getTarget().isPresent()) {
                AnnotationInstance orderAnnotation = bean.getTarget().get().declaredAnnotation(DotNames.COMPONENT_ORDER);
                if (orderAnnotation != null) {
                    AnnotationValue beforeValue = orderAnnotation.value("before");
                    if (beforeValue != null) {
                        beforeEdges.put(id, List.of(beforeValue.asStringArray()));
                    }
                    AnnotationValue afterValue = orderAnnotation.value("after");
                    if (afterValue != null) {
                        afterEdges.put(id, List.of(afterValue.asStringArray()));
                    }
                }
            }
        }

        if (allIds.isEmpty()) {
            return List.of();
        }
        return TopologicalSort.sort(allIds, beforeEdges, afterEdges, componentTypeName);
    }

    private static String readIdentifier(BeanInfo bean, String componentTypeName) {
        if (bean.getTarget().isEmpty()) {
            throw new IllegalStateException(componentTypeName + " bean has no target: " + bean);
        }
        Optional<AnnotationInstance> identifier = bean.getQualifier(DotNames.IDENTIFIER);
        if (identifier.isEmpty()) {
            throw new IllegalStateException(
                    componentTypeName + " bean " + bean.getBeanClass()
                            + " must be annotated with @io.smallrye.common.annotation.Identifier");
        }
        AnnotationValue value = identifier.get().value();
        if (value == null || value.asString().isBlank()) {
            throw new IllegalStateException(
                    componentTypeName + " bean " + bean.getBeanClass()
                            + " has an @Identifier annotation with an empty value");
        }
        return value.asString();
    }

    @BuildStep
    ReceiverExecutorImplementationBuildItem supportedExecutionModels(Capabilities capabilities) {
        if (capabilities.isPresent(Capability.VERTX)) {
            return new ReceiverExecutorImplementationBuildItem(VERTX);
        } else {
            // Note that quarkus-virtual-threads does require quarkus-vertx,
            // although it does not depend on the extension directly (to avoid the cycle)
            return new ReceiverExecutorImplementationBuildItem(DEFAULT_BLOCKING);
        }
    }

    @BuildStep
    void registerBeans(BuildProducer<AdditionalBeanBuildItem> beans,
            ReceiverExecutorImplementationBuildItem receiverExecutorImplementation) {
        AdditionalBeanBuildItem.Builder builder = AdditionalBeanBuildItem.builder();
        builder.addBeanClass(ReceiverManager.class);
        switch (receiverExecutorImplementation.getImplementation()) {
            case VERTX -> builder.addBeanClasses(VertxReceiverExecutor.class, VertxRequestContextInterceptor.class);
            case DEFAULT_BLOCKING -> builder.addBeanClasses(DefaultBlockingReceiverExecutor.class,
                    DefaultRequestContextInterceptor.class);
            default -> throw new IllegalArgumentException(
                    "Unexpected value: " + receiverExecutorImplementation.getImplementation());
        }
        beans.produce(builder.build());
    }

    private static ExecutionModel getExecutionModel(MethodInfo method) {
        if (method.hasDeclaredAnnotation(DotNames.RUN_ON_VIRTUAL_THREAD)) {
            return ExecutionModel.VIRTUAL_THREAD;
        } else if (method.hasDeclaredAnnotation(DotNames.BLOCKING)) {
            return ExecutionModel.BLOCKING;
        } else if (method.hasDeclaredAnnotation(DotNames.NON_BLOCKING)) {
            return ExecutionModel.NON_BLOCKING;
        } else {
            // Now test class-level annotations
            if (method.declaringClass().hasDeclaredAnnotation(DotNames.RUN_ON_VIRTUAL_THREAD)) {
                return ExecutionModel.VIRTUAL_THREAD;
            } else if (method.declaringClass().hasDeclaredAnnotation(DotNames.BLOCKING)) {
                return ExecutionModel.BLOCKING;
            } else if (method.declaringClass().hasDeclaredAnnotation(DotNames.NON_BLOCKING)) {
                return ExecutionModel.NON_BLOCKING;
            }
            return hasBlockingSignature(method) ? ExecutionModel.BLOCKING : ExecutionModel.NON_BLOCKING;
        }
    }

    static boolean hasBlockingSignature(MethodInfo method) {
        switch (method.returnType().kind()) {
            case VOID:
            case CLASS:
            case PRIMITIVE:
            case ARRAY:
                return true;
            case PARAMETERIZED_TYPE:
                // Uni and CompletionStage are non-blocking
                DotName name = method.returnType().asParameterizedType().name();
                return !name.equals(DotNames.UNI) && !name.equals(DotNames.COMPLETION_STAGE);
            default:
                throw new IllegalStateException(
                        "Unsupported return type:" + methodDesc(method));
        }
    }

    private static String methodDesc(MethodInfo method) {
        StringBuilder builder = new StringBuilder()
                .append(method.declaringClass().name().withoutPackagePrefix())
                .append("#")
                .append(method.name())
                .append('(');
        for (Iterator<org.jboss.jandex.Type> it = method.parameterTypes().iterator(); it.hasNext();) {
            builder.append(it.next());
            if (it.hasNext()) {
                builder.append(", ");
            }
        }
        builder.append(')');
        return builder.toString();
    }
}
