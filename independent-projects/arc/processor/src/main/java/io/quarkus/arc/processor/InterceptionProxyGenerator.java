package io.quarkus.arc.processor;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.interceptor.InvocationContext;

import org.jboss.jandex.AnnotationInstanceEquivalenceProxy;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.objectweb.asm.Opcodes;

import io.quarkus.arc.InjectableReferenceProvider;
import io.quarkus.arc.InterceptionProxy;
import io.quarkus.arc.InterceptionProxySubclass;
import io.quarkus.arc.impl.InterceptedMethodMetadata;
import io.quarkus.arc.processor.ResourceOutput.Resource;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FieldCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.FunctionCreator;
import io.quarkus.gizmo.Gizmo;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;

public class InterceptionProxyGenerator extends AbstractGenerator {
    private static final String INTERCEPTION_SUBCLASS = "_InterceptionSubclass";

    private final Predicate<DotName> applicationClassPredicate;
    private final IndexView beanArchiveIndex;
    private final AnnotationLiteralProcessor annotationLiterals;
    private final ReflectionRegistration reflectionRegistration;

    InterceptionProxyGenerator(boolean generateSources, Predicate<DotName> applicationClassPredicate,
            BeanDeployment deployment, AnnotationLiteralProcessor annotationLiterals,
            ReflectionRegistration reflectionRegistration) {
        super(generateSources);
        this.applicationClassPredicate = applicationClassPredicate;
        this.beanArchiveIndex = deployment.getBeanArchiveIndex();
        this.annotationLiterals = annotationLiterals;
        this.reflectionRegistration = reflectionRegistration;
    }

    Collection<Resource> generate(BeanInfo bean, Consumer<BytecodeTransformer> bytecodeTransformerConsumer,
            boolean transformUnproxyableClasses) {
        if (bean.getInterceptionProxy() == null) {
            return Collections.emptyList();
        }

        Function<String, Resource.SpecialType> specialTypeFunction = className -> {
            if (className.endsWith(INTERCEPTION_SUBCLASS)) {
                return Resource.SpecialType.SUBCLASS;
            }
            return null;
        };
        ResourceClassOutput classOutput = new ResourceClassOutput(applicationClassPredicate.test(bean.getBeanClass()),
                specialTypeFunction, generateSources);

        createInterceptionProxyProvider(classOutput, bean);
        createInterceptionProxy(classOutput, bean);
        createInterceptionSubclass(classOutput, bean.getInterceptionProxy(),
                bytecodeTransformerConsumer, transformUnproxyableClasses);

        return classOutput.getResources();
    }

    // ---

    static String interceptionProxyProviderName(BeanInfo bean) {
        return bean.getBeanClass().toString() + "_InterceptionProxyProvider_" + bean.getIdentifier();
    }

    private static String interceptionProxyName(BeanInfo bean) {
        return bean.getBeanClass().toString() + "_InterceptionProxy_" + bean.getIdentifier();
    }

    private static String interceptionSubclassName(InterceptionProxyInfo interceptionProxy) {
        return interceptionProxy.getTargetClass() + INTERCEPTION_SUBCLASS;
    }

    private void createInterceptionProxyProvider(ClassOutput classOutput, BeanInfo bean) {
        try (ClassCreator clazz = ClassCreator.builder()
                .classOutput(classOutput)
                .className(interceptionProxyProviderName(bean))
                .interfaces(Supplier.class, InjectableReferenceProvider.class)
                .build()) {

            // Supplier
            MethodCreator get0 = clazz.getMethodCreator("get", Object.class);
            get0.returnValue(get0.getThis());

            // InjectableReferenceProvider
            MethodCreator get1 = clazz.getMethodCreator("get", Object.class, CreationalContext.class);
            String targetName = interceptionProxyName(bean);
            ResultHandle result = get1.newInstance(MethodDescriptor.ofConstructor(targetName, CreationalContext.class),
                    get1.getMethodParam(0));
            get1.returnValue(result);
        }
    }

    private void createInterceptionProxy(ClassOutput classOutput, BeanInfo bean) {
        try (ClassCreator clazz = ClassCreator.builder()
                .classOutput(classOutput)
                .className(interceptionProxyName(bean))
                .interfaces(InterceptionProxy.class)
                .build()) {

            FieldCreator cc = clazz.getFieldCreator("creationalContext", CreationalContext.class)
                    .setModifiers(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL);

            MethodCreator ctor = clazz.getConstructorCreator(CreationalContext.class);
            ctor.invokeSpecialMethod(MethodDescriptor.ofConstructor(Object.class), ctor.getThis());
            ctor.writeInstanceField(cc.getFieldDescriptor(), ctor.getThis(), ctor.getMethodParam(0));
            ctor.returnVoid();

            MethodCreator create = clazz.getMethodCreator("create", Object.class, Object.class);

            ResultHandle ccHandle = create.readInstanceField(cc.getFieldDescriptor(), create.getThis());
            ResultHandle delegateHandle = create.getMethodParam(0);

            BytecodeCreator isInstance = create.ifFalse(
                    create.instanceOf(delegateHandle, bean.getInterceptionProxy().getTargetClass().toString()))
                    .falseBranch();
            isInstance.returnValue(isInstance.newInstance(MethodDescriptor.ofConstructor(
                    interceptionSubclassName(bean.getInterceptionProxy()), CreationalContext.class, Object.class),
                    ccHandle, delegateHandle));

            ResultHandle exceptionMessage = Gizmo.newStringBuilder(create)
                    .append("InterceptionProxy for ")
                    .append(create.load(bean.toString()))
                    .append(" got unknown delegate: ")
                    .append(delegateHandle)
                    .callToString();
            ResultHandle exception = create.newInstance(
                    MethodDescriptor.ofConstructor(IllegalArgumentException.class, String.class), exceptionMessage);
            create.throwException(exception);
            create.returnNull();
        }
    }

    private void createInterceptionSubclass(ClassOutput classOutput, InterceptionProxyInfo interceptionProxy,
            Consumer<BytecodeTransformer> bytecodeTransformerConsumer, boolean transformUnproxyableClasses) {
        BeanInfo pseudoBean = interceptionProxy.getPseudoBean();
        ClassInfo pseudoBeanClass = pseudoBean.getImplClazz();
        String pseudoBeanClassName = pseudoBeanClass.name().toString();
        boolean isInterface = pseudoBeanClass.isInterface();

        String superClass = isInterface ? Object.class.getName() : pseudoBeanClassName;
        String[] interfaces = isInterface
                ? new String[] { pseudoBeanClassName, InterceptionProxySubclass.class.getName() }
                : new String[] { InterceptionProxySubclass.class.getName() };

        try (ClassCreator clazz = ClassCreator.builder()
                .classOutput(classOutput)
                .className(interceptionSubclassName(interceptionProxy))
                .superClass(superClass)
                .interfaces(interfaces)
                .build()) {

            FieldCreator delegate = clazz.getFieldCreator("delegate", Object.class)
                    .setModifiers(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL);

            Map<String, ResultHandle> interceptorToResultHandle = new HashMap<>();
            Map<String, ResultHandle> interceptorInstanceToResultHandle = new HashMap<>();

            MethodCreator ctor = clazz.getConstructorCreator(CreationalContext.class, Object.class);
            ctor.invokeSpecialMethod(MethodDescriptor.ofConstructor(superClass), ctor.getThis());
            ctor.writeInstanceField(delegate.getFieldDescriptor(), ctor.getThis(), ctor.getMethodParam(1));
            ResultHandle arc = ctor.invokeStaticMethod(MethodDescriptors.ARC_CONTAINER);
            ResultHandle creationalContextHandle = ctor.getMethodParam(0);
            for (InterceptorInfo interceptorInfo : pseudoBean.getBoundInterceptors()) {
                ResultHandle interceptorBean = ctor.invokeInterfaceMethod(MethodDescriptors.ARC_CONTAINER_BEAN, arc,
                        ctor.load(interceptorInfo.getIdentifier()));
                interceptorToResultHandle.put(interceptorInfo.getIdentifier(), interceptorBean);

                ResultHandle creationalContext = ctor.invokeStaticMethod(MethodDescriptors.CREATIONAL_CTX_CHILD,
                        creationalContextHandle);
                ResultHandle interceptorInstance = ctor.invokeInterfaceMethod(MethodDescriptors.INJECTABLE_REF_PROVIDER_GET,
                        interceptorBean, creationalContext);
                interceptorInstanceToResultHandle.put(interceptorInfo.getIdentifier(), interceptorInstance);
            }

            Map<MethodDescriptor, MethodDescriptor> forwardingMethods = new HashMap<>();

            for (MethodInfo method : pseudoBean.getInterceptedMethods().keySet()) {
                forwardingMethods.put(MethodDescriptor.of(method), SubclassGenerator.createForwardingMethod(clazz,
                        pseudoBeanClassName, method, (bytecode, virtualMethod, params) -> {
                            ResultHandle delegateHandle = bytecode.readInstanceField(delegate.getFieldDescriptor(),
                                    bytecode.getThis());
                            return isInterface
                                    ? bytecode.invokeInterfaceMethod(virtualMethod, delegateHandle, params)
                                    : bytecode.invokeVirtualMethod(virtualMethod, delegateHandle, params);
                        }));
            }

            FieldCreator constructedField = clazz.getFieldCreator(SubclassGenerator.FIELD_NAME_CONSTRUCTED, boolean.class)
                    .setModifiers(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL);

            // Initialize maps of shared interceptor chains and interceptor bindings
            SubclassGenerator.IntegerHolder chainIdx = new SubclassGenerator.IntegerHolder();
            SubclassGenerator.IntegerHolder bindingIdx = new SubclassGenerator.IntegerHolder();
            Map<List<InterceptorInfo>, String> interceptorChainKeys = new HashMap<>();
            Map<Set<AnnotationInstanceEquivalenceProxy>, String> bindingKeys = new HashMap<>();

            ResultHandle interceptorChainMap = ctor.newInstance(MethodDescriptor.ofConstructor(HashMap.class));
            ResultHandle bindingsMap = ctor.newInstance(MethodDescriptor.ofConstructor(HashMap.class));

            // Shared interceptor bindings literals
            Map<AnnotationInstanceEquivalenceProxy, ResultHandle> bindingsLiterals = new HashMap<>();
            Function<Set<AnnotationInstanceEquivalenceProxy>, String> bindingsFun = SubclassGenerator.createBindingsFun(
                    bindingIdx, ctor, bindingsMap, bindingsLiterals, pseudoBean, annotationLiterals);
            Function<List<InterceptorInfo>, String> interceptorChainKeysFun = SubclassGenerator.createInterceptorChainKeysFun(
                    chainIdx, ctor, interceptorChainMap, interceptorInstanceToResultHandle, interceptorToResultHandle);

            int methodIdx = 1;
            for (BeanInfo.InterceptionInfo interception : pseudoBean.getInterceptedMethods().values()) {
                // Each intercepted method has a corresponding InterceptedMethodMetadata field
                clazz.getFieldCreator("arc$" + methodIdx++, InterceptedMethodMetadata.class.getName())
                        .setModifiers(Opcodes.ACC_PRIVATE);
                interceptorChainKeys.computeIfAbsent(interception.interceptors, interceptorChainKeysFun);
                bindingKeys.computeIfAbsent(interception.bindingsEquivalenceProxies(), bindingsFun);
            }

            // Split initialization of InterceptedMethodMetadata into multiple methods
            int group = 0;
            int groupLimit = 30;
            MethodCreator initMetadataMethod = null;

            // to avoid repeatedly looking for the exact same thing in the maps
            Map<String, ResultHandle> chainHandles = new HashMap<>();
            Map<String, ResultHandle> bindingsHandles = new HashMap<>();

            methodIdx = 1;
            for (MethodInfo method : pseudoBean.getInterceptedMethods().keySet()) {
                if (initMetadataMethod == null || methodIdx >= (group * groupLimit)) {
                    if (initMetadataMethod != null) {
                        // End the bytecode of the current initMetadata method
                        initMetadataMethod.returnVoid();
                        initMetadataMethod.close();
                        // Invoke arc$initMetadataX(interceptorChainMap,bindingsMap) in the ctor method
                        ctor.invokeVirtualMethod(initMetadataMethod.getMethodDescriptor(), ctor.getThis(),
                                interceptorChainMap, bindingsMap);
                    }
                    initMetadataMethod = clazz.getMethodCreator("arc$initMetadata" + group++, void.class, Map.class, Map.class)
                            .setModifiers(Opcodes.ACC_PRIVATE);
                    chainHandles.clear();
                    bindingsHandles.clear();
                }

                MethodDescriptor methodDescriptor = MethodDescriptor.of(method);
                BeanInfo.InterceptionInfo interception = pseudoBean.getInterceptedMethods().get(method);
                MethodDescriptor forwardDescriptor = forwardingMethods.get(methodDescriptor);
                List<Type> parameters = method.parameterTypes();

                final MethodCreator initMetadataMethodFinal = initMetadataMethod;

                // 1. Interceptor chain
                String interceptorChainKey = interceptorChainKeys.get(interception.interceptors);
                ResultHandle chainHandle = chainHandles.computeIfAbsent(interceptorChainKey, ignored -> {
                    return initMetadataMethodFinal.invokeInterfaceMethod(MethodDescriptors.MAP_GET,
                            initMetadataMethodFinal.getMethodParam(0), initMetadataMethodFinal.load(interceptorChainKey));
                });

                // 2. Method method = Reflections.findMethod(org.jboss.weld.arc.test.interceptors.SimpleBean.class,"foo",java.lang.String.class)
                ResultHandle[] paramsHandles = new ResultHandle[3];
                paramsHandles[0] = initMetadataMethod.loadClass(pseudoBeanClassName);
                paramsHandles[1] = initMetadataMethod.load(method.name());
                if (!parameters.isEmpty()) {
                    ResultHandle paramsArray = initMetadataMethod.newArray(Class.class,
                            initMetadataMethod.load(parameters.size()));
                    for (ListIterator<Type> iterator = parameters.listIterator(); iterator.hasNext();) {
                        initMetadataMethod.writeArrayValue(paramsArray, iterator.nextIndex(),
                                initMetadataMethod.loadClass(iterator.next().name().toString()));
                    }
                    paramsHandles[2] = paramsArray;
                } else {
                    paramsHandles[2] = initMetadataMethod
                            .readStaticField(FieldDescriptors.ANNOTATION_LITERALS_EMPTY_CLASS_ARRAY);
                }
                ResultHandle methodHandle = initMetadataMethod.invokeStaticMethod(MethodDescriptors.REFLECTIONS_FIND_METHOD,
                        paramsHandles);

                // 3. Interceptor bindings
                // Note that we use a shared set if possible
                String bindingKey = bindingKeys.get(interception.bindingsEquivalenceProxies());
                ResultHandle bindingsHandle = bindingsHandles.computeIfAbsent(bindingKey, ignored -> {
                    return initMetadataMethodFinal.invokeInterfaceMethod(MethodDescriptors.MAP_GET,
                            initMetadataMethodFinal.getMethodParam(1), initMetadataMethodFinal.load(bindingKey));
                });

                // Instantiate the forwarding function
                // BiFunction<Object, InvocationContext, Object> forward = (target, ctx) -> target.foo$$superforward((java.lang.String)ctx.getParameters()[0])
                FunctionCreator func = initMetadataMethod.createFunction(BiFunction.class);
                BytecodeCreator funcBytecode = func.getBytecode();
                ResultHandle targetHandle = funcBytecode.getMethodParam(0);
                ResultHandle ctxHandle = funcBytecode.getMethodParam(1);
                ResultHandle[] superParamHandles;
                if (parameters.isEmpty()) {
                    superParamHandles = new ResultHandle[0];
                } else {
                    superParamHandles = new ResultHandle[parameters.size()];
                    ResultHandle ctxParamsHandle = funcBytecode.invokeInterfaceMethod(
                            MethodDescriptor.ofMethod(InvocationContext.class, "getParameters", Object[].class),
                            ctxHandle);
                    // autoboxing is handled inside Gizmo
                    for (int i = 0; i < superParamHandles.length; i++) {
                        superParamHandles[i] = funcBytecode.readArrayValue(ctxParamsHandle, i);
                    }
                }

                ResultHandle superResult = isInterface
                        ? funcBytecode.invokeInterfaceMethod(methodDescriptor, targetHandle, superParamHandles)
                        : funcBytecode.invokeVirtualMethod(methodDescriptor, targetHandle, superParamHandles);
                funcBytecode.returnValue(superResult != null ? superResult : funcBytecode.loadNull());

                ResultHandle aroundForwardFun = func.getInstance();

                // Now create metadata for the given intercepted method
                ResultHandle methodMetadataHandle = initMetadataMethod.newInstance(
                        MethodDescriptors.INTERCEPTED_METHOD_METADATA_CONSTRUCTOR,
                        chainHandle, methodHandle, bindingsHandle, aroundForwardFun);

                FieldDescriptor metadataField = FieldDescriptor.of(clazz.getClassName(), "arc$" + methodIdx++,
                        InterceptedMethodMetadata.class.getName());

                initMetadataMethod.writeInstanceField(metadataField, initMetadataMethod.getThis(), methodMetadataHandle);

                // Needed when running on native image
                reflectionRegistration.registerMethod(method);

                // Finally create the intercepted method
                SubclassGenerator.createInterceptedMethod(method, clazz, metadataField, constructedField.getFieldDescriptor(),
                        forwardDescriptor, bc -> bc.readInstanceField(delegate.getFieldDescriptor(), bc.getThis()));
            }

            if (initMetadataMethod != null) {
                // Make sure we end the bytecode of the last initMetadata method
                initMetadataMethod.returnVoid();
                // Invoke arc$initMetadataX(interceptorChainMap,bindingsMap) in the ctor
                ctor.invokeVirtualMethod(initMetadataMethod.getMethodDescriptor(), ctor.getThis(),
                        interceptorChainMap, bindingsMap);
            }

            ctor.writeInstanceField(constructedField.getFieldDescriptor(), ctor.getThis(), ctor.load(true));
            ctor.returnVoid();

            MethodCreator getDelegate = clazz.getMethodCreator("arc_delegate", Object.class);
            getDelegate.returnValue(getDelegate.readInstanceField(delegate.getFieldDescriptor(), getDelegate.getThis()));

            // forward non-intercepted methods to the delegate unconditionally
            Collection<MethodInfo> methodsToForward = collectMethodsToForward(pseudoBean,
                    bytecodeTransformerConsumer, transformUnproxyableClasses);
            for (MethodInfo method : methodsToForward) {
                MethodCreator mc = clazz.getMethodCreator(MethodDescriptor.of(method));
                ResultHandle dlgt = mc.readInstanceField(delegate.getFieldDescriptor(), mc.getThis());
                ResultHandle[] args = new ResultHandle[method.parametersCount()];
                for (int i = 0; i < method.parametersCount(); i++) {
                    args[i] = mc.getMethodParam(i);
                }
                ResultHandle result = method.declaringClass().isInterface()
                        ? mc.invokeInterfaceMethod(method, dlgt, args)
                        : mc.invokeVirtualMethod(method, dlgt, args);
                mc.returnValue(result);
            }
        }
    }

    // uses the same algorithm as `ClientProxyGenerator`
    private Collection<MethodInfo> collectMethodsToForward(BeanInfo pseudoBean,
            Consumer<BytecodeTransformer> bytecodeTransformerConsumer, boolean transformUnproxyableClasses) {
        ClassInfo pseudoBeanClass = pseudoBean.getImplClazz();

        Map<Methods.MethodKey, MethodInfo> methods = new HashMap<>();
        Map<String, Set<Methods.MethodKey>> methodsFromWhichToRemoveFinal = new HashMap<>();

        Methods.addDelegatingMethods(beanArchiveIndex, pseudoBeanClass, methods, methodsFromWhichToRemoveFinal,
                transformUnproxyableClasses);

        if (!methodsFromWhichToRemoveFinal.isEmpty()) {
            for (Map.Entry<String, Set<Methods.MethodKey>> entry : methodsFromWhichToRemoveFinal.entrySet()) {
                String className = entry.getKey();
                bytecodeTransformerConsumer.accept(new BytecodeTransformer(className,
                        new Methods.RemoveFinalFromMethod(entry.getValue())));
            }
        }

        for (MethodInfo interceptedMethod : pseudoBean.getInterceptedMethods().keySet()) {
            // these methods are intercepted, so they don't need to (and in fact _must not_) forward directly
            methods.remove(new Methods.MethodKey(interceptedMethod));
        }

        return methods.values();
    }
}
