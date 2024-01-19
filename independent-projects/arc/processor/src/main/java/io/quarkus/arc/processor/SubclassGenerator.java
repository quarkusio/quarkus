package io.quarkus.arc.processor;

import static io.quarkus.arc.processor.IndexClassLookupUtils.getClassByName;
import static io.quarkus.arc.processor.KotlinUtils.isKotlinMethod;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_VOLATILE;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.InterceptionType;
import jakarta.interceptor.InvocationContext;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationInstanceEquivalenceProxy;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.jandex.Type.Kind;
import org.jboss.jandex.TypeVariable;

import io.quarkus.arc.ArcInvocationContext;
import io.quarkus.arc.ArcUndeclaredThrowableException;
import io.quarkus.arc.InjectableDecorator;
import io.quarkus.arc.InjectableInterceptor;
import io.quarkus.arc.Subclass;
import io.quarkus.arc.impl.InterceptedMethodMetadata;
import io.quarkus.arc.processor.BeanInfo.DecorationInfo;
import io.quarkus.arc.processor.BeanInfo.DecoratorMethod;
import io.quarkus.arc.processor.BeanInfo.InterceptionInfo;
import io.quarkus.arc.processor.BeanProcessor.PrivateMembersCollector;
import io.quarkus.arc.processor.Methods.MethodKey;
import io.quarkus.arc.processor.ResourceOutput.Resource;
import io.quarkus.arc.processor.ResourceOutput.Resource.SpecialType;
import io.quarkus.gizmo.AssignableResultHandle;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.CatchBlockCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.DescriptorUtils;
import io.quarkus.gizmo.FieldCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.FunctionCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.gizmo.TryBlock;

/**
 * A subclass is generated for any intercepted/decorated bean.
 */
public class SubclassGenerator extends AbstractGenerator {

    private static final DotName JAVA_LANG_THROWABLE = DotNames.create(Throwable.class.getName());
    private static final DotName JAVA_LANG_EXCEPTION = DotNames.create(Exception.class.getName());
    private static final DotName JAVA_LANG_RUNTIME_EXCEPTION = DotNames.create(RuntimeException.class.getName());

    static final String SUBCLASS_SUFFIX = "_Subclass";
    static final String MARK_CONSTRUCTED_METHOD_NAME = "arc$markConstructed";
    static final String DESTROY_METHOD_NAME = "arc$destroy";

    protected static final String FIELD_NAME_PREDESTROYS = "arc$preDestroys";
    protected static final String FIELD_NAME_CONSTRUCTED = "arc$constructed";

    private final Predicate<DotName> applicationClassPredicate;
    private final Set<String> existingClasses;
    private final PrivateMembersCollector privateMembers;

    static String generatedName(DotName providerTypeName, String baseName) {
        String packageName = DotNames.internalPackageNameWithTrailingSlash(providerTypeName);
        return packageName + baseName + SUBCLASS_SUFFIX;
    }

    private final AnnotationLiteralProcessor annotationLiterals;

    public SubclassGenerator(AnnotationLiteralProcessor annotationLiterals, Predicate<DotName> applicationClassPredicate,
            boolean generateSources, ReflectionRegistration reflectionRegistration,
            Set<String> existingClasses, PrivateMembersCollector privateMembers) {
        super(generateSources, reflectionRegistration);
        this.applicationClassPredicate = applicationClassPredicate;
        this.annotationLiterals = annotationLiterals;
        this.existingClasses = existingClasses;
        this.privateMembers = privateMembers;
    }

    Collection<Resource> generate(BeanInfo bean, String beanClassName) {

        Type providerType = bean.getProviderType();
        ClassInfo providerClass = getClassByName(bean.getDeployment().getBeanArchiveIndex(), providerType.name());
        String providerTypeName = providerClass.name().toString();
        String baseName = getBaseName(bean, beanClassName);
        String generatedName = generatedName(providerType.name(), baseName);
        if (existingClasses.contains(generatedName)) {
            return Collections.emptyList();
        }

        ResourceClassOutput classOutput = new ResourceClassOutput(applicationClassPredicate.test(bean.getBeanClass()),
                name -> name.equals(generatedName) ? SpecialType.SUBCLASS : null,
                generateSources);

        // Foo_Subclass extends Foo implements Subclass
        ClassCreator subclass = ClassCreator.builder().classOutput(classOutput).className(generatedName)
                .superClass(providerTypeName).interfaces(Subclass.class)
                .build();

        FieldDescriptor preDestroyField = createConstructor(classOutput, bean, subclass, providerType, providerTypeName,
                reflectionRegistration);
        createDestroy(classOutput, bean, subclass, preDestroyField);

        subclass.close();
        return classOutput.getResources();
    }

    protected FieldDescriptor createConstructor(ClassOutput classOutput, BeanInfo bean, ClassCreator subclass,
            Type providerType, String providerTypeName, ReflectionRegistration reflectionRegistration) {

        // Constructor parameters
        List<String> parameterTypes = new ArrayList<>();
        // First constructor injection points
        Optional<Injection> constructorInjection = bean.getConstructorInjection();
        if (constructorInjection.isPresent()) {
            for (InjectionPointInfo injectionPoint : constructorInjection.get().injectionPoints) {
                parameterTypes.add(injectionPoint.getType().name().toString());
            }
        }
        int superParamsSize = parameterTypes.size();
        // CreationalContext
        parameterTypes.add(CreationalContext.class.getName());
        // Interceptor providers
        List<InterceptorInfo> boundInterceptors = bean.getBoundInterceptors();
        for (int j = 0; j < boundInterceptors.size(); j++) {
            parameterTypes.add(InjectableInterceptor.class.getName());
        }
        // Decorator providers
        List<DecoratorInfo> boundDecorators = bean.getBoundDecorators();
        for (int j = 0; j < boundDecorators.size(); j++) {
            parameterTypes.add(InjectableDecorator.class.getName());
        }
        MethodCreator constructor = subclass.getMethodCreator(Methods.INIT, "V", parameterTypes.toArray(new String[0]));

        ResultHandle creationalContextHandle = constructor.getMethodParam(superParamsSize);
        ResultHandle[] superParams = new ResultHandle[superParamsSize];
        for (int j = 0; j < superParamsSize; j++) {
            superParams[j] = constructor.getMethodParam(j);
        }
        // super(fooProvider)
        constructor.invokeSpecialMethod(
                MethodDescriptor.ofConstructor(providerTypeName,
                        parameterTypes.subList(0, superParamsSize).toArray(new String[0])),
                constructor.getThis(), superParams);

        // First instantiate all interceptor instances, so that they can be shared
        Map<String, ResultHandle> interceptorToResultHandle = new HashMap<>();
        Map<String, ResultHandle> interceptorInstanceToResultHandle = new HashMap<>();
        for (int j = 0; j < boundInterceptors.size(); j++) {
            InterceptorInfo interceptorInfo = boundInterceptors.get(j);

            ResultHandle constructorMethodParam = constructor.getMethodParam(j + superParamsSize + 1);
            interceptorToResultHandle.put(interceptorInfo.getIdentifier(), constructorMethodParam);

            // create instance of each interceptor -> InjectableInterceptor.get()
            ResultHandle creationalContext = constructor.invokeStaticMethod(MethodDescriptors.CREATIONAL_CTX_CHILD,
                    creationalContextHandle);
            ResultHandle interceptorInstance = constructor.invokeInterfaceMethod(
                    MethodDescriptors.INJECTABLE_REF_PROVIDER_GET, constructorMethodParam, creationalContext);

            interceptorInstanceToResultHandle.put(interceptorInfo.getIdentifier(), interceptorInstance);
        }

        Map<MethodDescriptor, MethodDescriptor> forwardingMethods = new HashMap<>();
        List<MethodInfo> interceptedOrDecoratedMethods = bean.getInterceptedOrDecoratedMethods();
        for (MethodInfo method : interceptedOrDecoratedMethods) {
            forwardingMethods.put(MethodDescriptor.of(method),
                    createForwardingMethod(subclass, providerTypeName, method));
        }

        // If a decorator is associated:
        // 1. Generate the delegate subclass
        // 2. Instantiate the decorator instance, add set the corresponding field
        Map<String, ResultHandle> decoratorToResultHandle;
        if (boundDecorators.isEmpty()) {
            decoratorToResultHandle = Collections.emptyMap();
        } else {
            decoratorToResultHandle = new HashMap<>();
            for (int j = 0; j < boundDecorators.size(); j++) {
                processDecorator(boundDecorators.get(j), bean, providerType, providerTypeName, subclass, classOutput,
                        constructor, boundInterceptors.size() + superParamsSize + j + 1, decoratorToResultHandle,
                        creationalContextHandle,
                        forwardingMethods);
            }
        }

        // PreDestroy interceptors
        FieldCreator preDestroysField = null;
        InterceptionInfo preDestroys = bean.getLifecycleInterceptors(InterceptionType.PRE_DESTROY);
        if (!preDestroys.isEmpty()) {
            // private final List<InvocationContextImpl.InterceptorInvocation> preDestroys
            preDestroysField = subclass
                    .getFieldCreator(FIELD_NAME_PREDESTROYS, DescriptorUtils.extToInt(ArrayList.class.getName()))
                    .setModifiers(ACC_PRIVATE | ACC_FINAL);
            // preDestroys = new ArrayList<>()
            constructor.writeInstanceField(preDestroysField.getFieldDescriptor(), constructor.getThis(),
                    constructor.newInstance(MethodDescriptor.ofConstructor(ArrayList.class)));
            for (InterceptorInfo interceptor : preDestroys.interceptors) {
                // preDestroys.add(InvocationContextImpl.InterceptorInvocation.preDestroy(provider1,interceptorInstanceMap.get(InjectableInterceptor.getIdentifier())))
                ResultHandle interceptorInstance = interceptorInstanceToResultHandle.get(interceptor.getIdentifier());
                ResultHandle interceptionInvocation = constructor.invokeStaticMethod(
                        MethodDescriptors.INTERCEPTOR_INVOCATION_PRE_DESTROY,
                        interceptorToResultHandle.get(interceptor.getIdentifier()),
                        interceptorInstance);
                constructor.invokeInterfaceMethod(MethodDescriptors.LIST_ADD,
                        constructor.readInstanceField(preDestroysField.getFieldDescriptor(), constructor.getThis()),
                        interceptionInvocation);
            }
        }

        // `volatile` is perhaps not best, this field is monotonic (once `true`, it never becomes `false` again),
        // so maybe making the `markConstructed` method `synchronized` would be enough (?)
        FieldCreator constructedField = subclass.getFieldCreator(FIELD_NAME_CONSTRUCTED, boolean.class)
                .setModifiers(ACC_PRIVATE | ACC_VOLATILE);

        MethodCreator markConstructed = subclass.getMethodCreator(MARK_CONSTRUCTED_METHOD_NAME, void.class);
        markConstructed.writeInstanceField(constructedField.getFieldDescriptor(), markConstructed.getThis(),
                markConstructed.load(true));
        markConstructed.returnVoid();

        // Initialize maps of shared interceptor chains and interceptor bindings
        IntegerHolder chainIdx = new IntegerHolder();
        IntegerHolder bindingIdx = new IntegerHolder();
        Map<List<InterceptorInfo>, String> interceptorChainKeys = new HashMap<>();
        Map<Set<AnnotationInstanceEquivalenceProxy>, String> bindingKeys = new HashMap<>();

        ResultHandle interceptorChainMap = constructor.newInstance(MethodDescriptor.ofConstructor(HashMap.class));
        ResultHandle bindingsMap = constructor.newInstance(MethodDescriptor.ofConstructor(HashMap.class));

        // Shared interceptor bindings literals
        Map<AnnotationInstanceEquivalenceProxy, ResultHandle> bindingsLiterals = new HashMap<>();
        Function<AnnotationInstanceEquivalenceProxy, ResultHandle> bindingsLiteralFun = new Function<AnnotationInstanceEquivalenceProxy, ResultHandle>() {
            @Override
            public ResultHandle apply(AnnotationInstanceEquivalenceProxy binding) {
                // Create annotation literal if needed
                ClassInfo bindingClass = bean.getDeployment().getInterceptorBinding(binding.get().name());
                return annotationLiterals.create(constructor, bindingClass, binding.get());
            }
        };

        Function<List<InterceptorInfo>, String> interceptorChainKeysFun = new Function<List<InterceptorInfo>, String>() {
            @Override
            public String apply(List<InterceptorInfo> interceptors) {
                String key = "i" + chainIdx.i++;
                if (interceptors.size() == 1) {
                    // List<InvocationContextImpl.InterceptorInvocation> chain = Collections.singletonList(...);
                    InterceptorInfo interceptor = interceptors.get(0);
                    ResultHandle interceptorInstance = interceptorInstanceToResultHandle.get(interceptor.getIdentifier());
                    ResultHandle interceptionInvocation = constructor.invokeStaticMethod(
                            MethodDescriptors.INTERCEPTOR_INVOCATION_AROUND_INVOKE,
                            interceptorToResultHandle.get(interceptor.getIdentifier()), interceptorInstance);
                    constructor.invokeInterfaceMethod(MethodDescriptors.MAP_PUT, interceptorChainMap, constructor.load(key),
                            constructor.invokeStaticMethod(MethodDescriptors.COLLECTIONS_SINGLETON_LIST,
                                    interceptionInvocation));
                } else {
                    // List<InvocationContextImpl.InterceptorInvocation> chain = new ArrayList<>();
                    ResultHandle chainHandle = constructor.newInstance(MethodDescriptor.ofConstructor(ArrayList.class));
                    for (InterceptorInfo interceptor : interceptors) {
                        // m1Chain.add(InvocationContextImpl.InterceptorInvocation.aroundInvoke(p3,interceptorInstanceMap.get(InjectableInterceptor.getIdentifier())))
                        ResultHandle interceptorInstance = interceptorInstanceToResultHandle.get(interceptor.getIdentifier());
                        ResultHandle interceptionInvocation = constructor.invokeStaticMethod(
                                MethodDescriptors.INTERCEPTOR_INVOCATION_AROUND_INVOKE,
                                interceptorToResultHandle.get(interceptor.getIdentifier()), interceptorInstance);
                        constructor.invokeInterfaceMethod(MethodDescriptors.LIST_ADD, chainHandle, interceptionInvocation);
                    }
                    constructor.invokeInterfaceMethod(MethodDescriptors.MAP_PUT, interceptorChainMap, constructor.load(key),
                            chainHandle);
                }
                return key;
            }
        };

        Function<Set<AnnotationInstanceEquivalenceProxy>, String> bindingsFun = new Function<Set<AnnotationInstanceEquivalenceProxy>, String>() {
            @Override
            public String apply(Set<AnnotationInstanceEquivalenceProxy> bindings) {
                String key = "b" + bindingIdx.i++;
                if (bindings.size() == 1) {
                    constructor.invokeInterfaceMethod(MethodDescriptors.MAP_PUT, bindingsMap, constructor.load(key),
                            constructor.invokeStaticMethod(MethodDescriptors.COLLECTIONS_SINGLETON,
                                    bindingsLiterals.computeIfAbsent(bindings.iterator().next(), bindingsLiteralFun)));
                } else {
                    ResultHandle bindingsArray = constructor.newArray(Object.class, bindings.size());
                    int bindingsIndex = 0;
                    for (AnnotationInstanceEquivalenceProxy binding : bindings) {
                        constructor.writeArrayValue(bindingsArray, bindingsIndex++,
                                bindingsLiterals.computeIfAbsent(binding, bindingsLiteralFun));
                    }
                    constructor.invokeInterfaceMethod(MethodDescriptors.MAP_PUT, bindingsMap, constructor.load(key),
                            constructor.invokeStaticMethod(MethodDescriptors.SETS_OF, bindingsArray));
                }
                return key;
            }
        };

        int methodIdx = 1;
        for (MethodInfo method : interceptedOrDecoratedMethods) {
            InterceptionInfo interception = bean.getInterceptedMethods().get(method);
            if (interception != null) {
                // Each intercepted method has a corresponding InterceptedMethodMetadata field
                subclass.getFieldCreator("arc$" + methodIdx++, InterceptedMethodMetadata.class.getName())
                        .setModifiers(ACC_PRIVATE);
                interceptorChainKeys.computeIfAbsent(interception.interceptors, interceptorChainKeysFun);
                bindingKeys.computeIfAbsent(interception.bindingsEquivalenceProxies(), bindingsFun);
            }
        }

        // Initialize the "aroundInvokes" field if necessary
        if (bean.hasAroundInvokes()) {
            FieldCreator field = subclass.getFieldCreator("aroundInvokes", List.class)
                    .setModifiers(ACC_PRIVATE);
            ResultHandle methodsList = constructor.newInstance(MethodDescriptor.ofConstructor(ArrayList.class));
            for (MethodInfo method : bean.getAroundInvokes()) {
                // BiFunction<Object,InvocationContext,Object>
                FunctionCreator fun = constructor.createFunction(BiFunction.class);
                BytecodeCreator funBytecode = fun.getBytecode();
                ResultHandle ret = invokeInterceptorMethod(funBytecode, method,
                        applicationClassPredicate.test(bean.getBeanClass()),
                        funBytecode.getMethodParam(1),
                        funBytecode.getMethodParam(0));
                funBytecode.returnValue(ret);
                constructor.invokeInterfaceMethod(MethodDescriptors.LIST_ADD, methodsList, fun.getInstance());
            }
            constructor.writeInstanceField(field.getFieldDescriptor(), constructor.getThis(), methodsList);
        }

        // Split initialization of InterceptedMethodMetadata into multiple methods
        int group = 0;
        int groupLimit = 30;
        MethodCreator initMetadataMethod = null;

        // to avoid repeatedly looking for the exact same thing in the maps
        Map<String, ResultHandle> chainHandles = new HashMap<>();
        Map<String, ResultHandle> bindingsHandles = new HashMap<>();

        methodIdx = 1;
        for (MethodInfo method : interceptedOrDecoratedMethods) {
            if (initMetadataMethod == null || methodIdx >= (group * groupLimit)) {
                if (initMetadataMethod != null) {
                    // End the bytecode of the current initMetadata method
                    initMetadataMethod.returnVoid();
                    initMetadataMethod.close();
                    // Invoke arc$initMetadataX(interceptorChainMap,bindingsMap) in the constructor
                    constructor.invokeVirtualMethod(initMetadataMethod.getMethodDescriptor(), constructor.getThis(),
                            interceptorChainMap, bindingsMap);
                }
                initMetadataMethod = subclass.getMethodCreator("arc$initMetadata" + group++, void.class, Map.class, Map.class)
                        .setModifiers(ACC_PRIVATE);
                chainHandles.clear();
                bindingsHandles.clear();
            }

            MethodDescriptor methodDescriptor = MethodDescriptor.of(method);
            MethodDescriptor originalMethodDescriptor = MethodDescriptor.of(method);
            InterceptionInfo interception = bean.getInterceptedMethods().get(method);
            DecorationInfo decoration = bean.getDecoratedMethods().get(method);
            MethodDescriptor forwardDescriptor = forwardingMethods.get(methodDescriptor);
            List<Type> parameters = method.parameterTypes();

            if (interception != null) {
                final MethodCreator initMetadataMethodFinal = initMetadataMethod;

                // 1. Interceptor chain
                String interceptorChainKey = interceptorChainKeys.get(interception.interceptors);
                ResultHandle chainHandle = chainHandles.computeIfAbsent(interceptorChainKey, ignored -> {
                    return initMetadataMethodFinal.invokeInterfaceMethod(MethodDescriptors.MAP_GET,
                            initMetadataMethodFinal.getMethodParam(0), initMetadataMethodFinal.load(interceptorChainKey));
                });

                // 2. Method method = Reflections.findMethod(org.jboss.weld.arc.test.interceptors.SimpleBean.class,"foo",java.lang.String.class)
                ResultHandle[] paramsHandles = new ResultHandle[3];
                paramsHandles[0] = initMetadataMethod.loadClass(providerTypeName);
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

                DecoratorMethod decoratorMethod = decoration != null ? decoration.firstDecoratorMethod() : null;
                ResultHandle decoratorHandle = null;
                if (decoratorMethod != null) {
                    decoratorHandle = initMetadataMethod.readInstanceField(FieldDescriptor.of(subclass.getClassName(),
                            decoratorMethod.decorator.getIdentifier(), Object.class.getName()), initMetadataMethod.getThis());
                }

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

                // If a decorator is bound then invoke the method upon the decorator instance instead of the generated forwarding method
                if (decoratorMethod != null) {
                    AssignableResultHandle funDecoratorInstance = funcBytecode.createVariable(Object.class);
                    funcBytecode.assign(funDecoratorInstance, decoratorHandle);
                    String declaringClass = decoratorMethod.decorator.getBeanClass().toString();
                    if (decoratorMethod.decorator.isAbstract()) {
                        String baseName = DecoratorGenerator
                                .createBaseName(decoratorMethod.decorator.getTarget().get().asClass());
                        String targetPackage = DotNames.packageName(decoratorMethod.decorator.getProviderType().name());
                        declaringClass = generatedNameFromTarget(targetPackage, baseName,
                                DecoratorGenerator.ABSTRACT_IMPL_SUFFIX);
                    }
                    // We need to use the decorator method in order to support generic decorators
                    MethodDescriptor decoratorMethodDescriptor = MethodDescriptor.of(decoratorMethod.method);
                    MethodDescriptor virtualMethodDescriptor = MethodDescriptor.ofMethod(declaringClass,
                            originalMethodDescriptor.getName(),
                            decoratorMethodDescriptor.getReturnType(), decoratorMethodDescriptor.getParameterTypes());
                    funcBytecode
                            .returnValue(funcBytecode.invokeVirtualMethod(virtualMethodDescriptor, funDecoratorInstance,
                                    superParamHandles));
                } else {
                    ResultHandle superResult = funcBytecode.invokeVirtualMethod(forwardDescriptor, targetHandle,
                            superParamHandles);
                    funcBytecode.returnValue(superResult != null ? superResult : funcBytecode.loadNull());
                }

                ResultHandle aroundForwardFun = func.getInstance();

                if (bean.hasAroundInvokes()) {
                    // Wrap the forwarding function with a function that calls around invoke methods declared in a hierarchy of the target class first
                    AssignableResultHandle methodsList = initMetadataMethod.createVariable(List.class);
                    initMetadataMethod.assign(methodsList, initMetadataMethod.readInstanceField(
                            FieldDescriptor.of(subclass.getClassName(), "aroundInvokes", List.class),
                            initMetadataMethod.getThis()));
                    FunctionCreator targetFun = initMetadataMethod.createFunction(BiFunction.class);
                    BytecodeCreator targetFunBytecode = targetFun.getBytecode();
                    ResultHandle ret = targetFunBytecode.invokeStaticMethod(
                            MethodDescriptors.INVOCATION_CONTEXTS_PERFORM_TARGET_AROUND_INVOKE,
                            targetFunBytecode.getMethodParam(1),
                            methodsList, aroundForwardFun);
                    targetFunBytecode.returnValue(ret);
                    aroundForwardFun = targetFun.getInstance();
                }

                // Now create metadata for the given intercepted method
                ResultHandle methodMetadataHandle = initMetadataMethod.newInstance(
                        MethodDescriptors.INTERCEPTED_METHOD_METADATA_CONSTRUCTOR,
                        chainHandle, methodHandle, bindingsHandle, aroundForwardFun);

                FieldDescriptor metadataField = FieldDescriptor.of(subclass.getClassName(), "arc$" + methodIdx++,
                        InterceptedMethodMetadata.class.getName());

                initMetadataMethod.writeInstanceField(metadataField, initMetadataMethod.getThis(), methodMetadataHandle);

                // Needed when running on native image
                reflectionRegistration.registerMethod(method);

                // Finally create the intercepted method
                createInterceptedMethod(bean, method, subclass, providerTypeName, metadataField,
                        constructedField.getFieldDescriptor(), forwardDescriptor);
            } else {
                // Only decorators are applied
                MethodCreator decoratedMethod = subclass.getMethodCreator(methodDescriptor);

                ResultHandle[] params = new ResultHandle[parameters.size()];
                for (int i = 0; i < parameters.size(); ++i) {
                    params[i] = decoratedMethod.getMethodParam(i);
                }

                // Delegate to super class if not constructed yet
                BytecodeCreator notConstructed = decoratedMethod
                        .ifFalse(decoratedMethod.readInstanceField(constructedField.getFieldDescriptor(),
                                decoratedMethod.getThis()))
                        .trueBranch();
                if (Modifier.isAbstract(method.flags())) {
                    notConstructed.throwException(IllegalStateException.class, "Cannot delegate to an abstract method");
                } else {
                    notConstructed.returnValue(
                            notConstructed.invokeVirtualMethod(forwardDescriptor, notConstructed.getThis(), params));
                }

                DecoratorMethod decoratorMethod = decoration.firstDecoratorMethod();
                DecoratorInfo firstDecorator = decoratorMethod.decorator;
                ResultHandle decoratorInstance = decoratedMethod.readInstanceField(FieldDescriptor.of(subclass.getClassName(),
                        firstDecorator.getIdentifier(), Object.class.getName()), decoratedMethod.getThis());

                String declaringClass = firstDecorator.getBeanClass().toString();
                if (firstDecorator.isAbstract()) {
                    String baseName = DecoratorGenerator.createBaseName(firstDecorator.getTarget().get().asClass());
                    String targetPackage = DotNames.packageName(firstDecorator.getProviderType().name());
                    declaringClass = generatedNameFromTarget(targetPackage, baseName, DecoratorGenerator.ABSTRACT_IMPL_SUFFIX);
                }
                // We need to use the decorator method in order to support generic decorators
                MethodDescriptor decoratorMethodDescriptor = MethodDescriptor.of(decoratorMethod.method);
                MethodDescriptor virtualMethodDescriptor = MethodDescriptor.ofMethod(
                        declaringClass, methodDescriptor.getName(),
                        decoratorMethodDescriptor.getReturnType(), decoratorMethodDescriptor.getParameterTypes());
                decoratedMethod
                        .returnValue(decoratedMethod.invokeVirtualMethod(virtualMethodDescriptor, decoratorInstance, params));
            }
        }

        if (initMetadataMethod != null) {
            // Make sure we end the bytecode of the last initMetadata method
            initMetadataMethod.returnVoid();
            // Invoke arc$initMetadataX(interceptorChainMap,bindingsMap) in the constructor
            constructor.invokeVirtualMethod(initMetadataMethod.getMethodDescriptor(), constructor.getThis(),
                    interceptorChainMap, bindingsMap);
        }

        constructor.returnValue(null);
        return preDestroysField != null ? preDestroysField.getFieldDescriptor() : null;
    }

    private ResultHandle invokeInterceptorMethod(BytecodeCreator creator, MethodInfo interceptorMethod,
            boolean isApplicationClass, ResultHandle invocationContext, ResultHandle targetInstance) {
        ResultHandle ret;
        // Check if interceptor method uses InvocationContext or ArcInvocationContext
        Class<?> invocationContextClass;
        if (interceptorMethod.parameterType(0).name().equals(DotNames.INVOCATION_CONTEXT)) {
            invocationContextClass = InvocationContext.class;
        } else {
            invocationContextClass = ArcInvocationContext.class;
        }
        if (Modifier.isPrivate(interceptorMethod.flags())) {
            privateMembers.add(isApplicationClass,
                    String.format("Interceptor method %s#%s()", interceptorMethod.declaringClass().name(),
                            interceptorMethod.name()));
            // Use reflection fallback
            ResultHandle paramTypesArray = creator.newArray(Class.class, creator.load(1));
            creator.writeArrayValue(paramTypesArray, 0, creator.loadClass(invocationContextClass));
            ResultHandle argsArray = creator.newArray(Object.class, creator.load(1));
            creator.writeArrayValue(argsArray, 0, invocationContext);
            reflectionRegistration.registerMethod(interceptorMethod);
            ret = creator.invokeStaticMethod(MethodDescriptors.REFLECTIONS_INVOKE_METHOD,
                    creator.loadClass(interceptorMethod.declaringClass()
                            .name()
                            .toString()),
                    creator.load(interceptorMethod.name()), paramTypesArray, targetInstance, argsArray);
        } else {
            ret = creator.invokeVirtualMethod(interceptorMethod, targetInstance, invocationContext);
        }
        return ret;
    }

    private void processDecorator(DecoratorInfo decorator, BeanInfo bean, Type providerType,
            String providerTypeName, ClassCreator subclass, ClassOutput classOutput, MethodCreator subclassConstructor,
            int paramIndex, Map<String, ResultHandle> decoratorToResultHandle,
            ResultHandle creationalContextHandle, Map<MethodDescriptor, MethodDescriptor> forwardingMethods) {

        // First genetare the delegate subclass
        // An instance of this subclass is injected in the delegate injection point of a decorator instance
        ClassInfo decoratorClass = decorator.getTarget().get().asClass();
        String baseName;
        if (decoratorClass.enclosingClass() != null) {
            baseName = DotNames.simpleName(decoratorClass.enclosingClass()) + UNDERSCORE
                    + DotNames.simpleName(decoratorClass);
        } else {
            baseName = DotNames.simpleName(decoratorClass);
        }
        // Name: AlphaDecorator_FooBeanId_Delegate_Subclass
        String generatedName = generatedName(providerType.name(),
                baseName + UNDERSCORE + bean.getIdentifier() + UNDERSCORE + "Delegate");

        ClassCreator.Builder delegateSubclassBuilder = ClassCreator.builder().classOutput(classOutput)
                .className(generatedName);
        ClassInfo delegateTypeClass = decorator.getDelegateTypeClass();
        boolean delegateTypeIsInterface = Modifier.isInterface(delegateTypeClass.flags());
        // The subclass implements/extends the delegate type
        if (delegateTypeIsInterface) {
            delegateSubclassBuilder.interfaces(delegateTypeClass.name().toString());
        } else {
            delegateSubclassBuilder.superClass(delegateTypeClass.name().toString());
        }
        ClassCreator delegateSubclass = delegateSubclassBuilder.build();

        Map<MethodDescriptor, DecoratorMethod> nextDecorators = bean.getNextDecorators(decorator);
        Set<DecoratorInfo> nextDecoratorsValues = new HashSet<>();
        for (DecoratorMethod decoratorMethod : nextDecorators.values()) {
            nextDecoratorsValues.add(decoratorMethod.decorator);
        }
        List<DecoratorInfo> decoratorParameters = new ArrayList<>(nextDecoratorsValues);
        Collections.sort(decoratorParameters);
        Set<MethodInfo> decoratedMethods = bean.getDecoratedMethods(decorator);
        Set<MethodDescriptor> decoratedMethodDescriptors = new HashSet<>(decoratedMethods.size());
        for (MethodInfo m : decoratedMethods) {
            decoratedMethodDescriptors.add(MethodDescriptor.of(m));
        }

        List<String> constructorParameterTypes = new ArrayList<>();
        // Holds a reference to the subclass of the decorated bean
        FieldCreator subclassField = delegateSubclass.getFieldCreator("subclass", subclass.getClassName())
                .setModifiers(ACC_PRIVATE | ACC_FINAL);
        constructorParameterTypes.add(subclass.getClassName());
        Map<DecoratorInfo, FieldDescriptor> nextDecoratorToField = new HashMap<>();
        for (DecoratorInfo nextDecorator : decoratorParameters) {
            FieldCreator nextDecoratorField = delegateSubclass
                    .getFieldCreator(nextDecorator.getIdentifier(), nextDecorator.getBeanClass().toString())
                    .setModifiers(ACC_PRIVATE | ACC_FINAL);
            constructorParameterTypes.add(nextDecorator.getBeanClass().toString());
            nextDecoratorToField.put(nextDecorator, nextDecoratorField.getFieldDescriptor());
        }

        // Constructor
        MethodCreator constructor = delegateSubclass.getMethodCreator(Methods.INIT, "V",
                constructorParameterTypes.toArray(new String[0]));
        int param = 0;
        if (delegateTypeIsInterface) {
            // Invoke super()
            constructor.invokeSpecialMethod(MethodDescriptors.OBJECT_CONSTRUCTOR, constructor.getThis());
        } else {
            constructor.invokeSpecialMethod(MethodDescriptor.ofConstructor(delegateTypeClass.name().toString()),
                    constructor.getThis());
        }
        // Set fields
        constructor.writeInstanceField(
                subclassField.getFieldDescriptor(), constructor.getThis(), constructor.getMethodParam(param++));
        for (FieldDescriptor field : nextDecoratorToField.values()) {
            constructor.writeInstanceField(
                    field, constructor.getThis(), constructor.getMethodParam(param++));
        }
        constructor.returnValue(null);

        IndexView index = bean.getDeployment().getBeanArchiveIndex();

        // Identify the set of methods that should be delegated
        // Note that the delegate subclass must override ALL methods from the delegate type
        // This is not enough if the delegate type is parameterized
        Set<MethodKey> methods = new HashSet<>();
        Methods.addDelegateTypeMethods(index, delegateTypeClass, methods);

        // The delegate type can declare type parameters
        // For example @Delegate Converter<String> should result in a T -> String mapping
        List<TypeVariable> typeParameters = delegateTypeClass.typeParameters();
        Map<String, Type> resolvedTypeParameters = Collections.emptyMap();
        if (!typeParameters.isEmpty()) {
            resolvedTypeParameters = new HashMap<>();
            // The delegate type can be used to infer the parameter types
            Type delegateType = decorator.getDelegateType();
            if (delegateType.kind() == Kind.PARAMETERIZED_TYPE) {
                List<Type> typeArguments = delegateType.asParameterizedType().arguments();
                for (int i = 0; i < typeParameters.size(); i++) {
                    resolvedTypeParameters.put(typeParameters.get(i).identifier(), typeArguments.get(i));
                }
            }
        }

        for (MethodKey m : methods) {
            MethodInfo method = m.method;
            MethodDescriptor methodDescriptor = MethodDescriptor.of(method);
            MethodCreator forward = delegateSubclass.getMethodCreator(methodDescriptor);
            // Exceptions
            for (Type exception : method.exceptions()) {
                forward.addException(exception.toString());
            }

            ResultHandle ret = null;
            ResultHandle delegateTo;

            // Method params
            List<Type> parameters = method.parameterTypes();
            ResultHandle[] params = new ResultHandle[parameters.size()];
            for (int i = 0; i < parameters.size(); ++i) {
                params[i] = forward.getMethodParam(i);
            }

            // Create a resolved descriptor variant if a param contains a type variable
            // E.g. ping(T) -> ping(String)
            MethodDescriptor resolvedMethodDescriptor;
            if (typeParameters.isEmpty()
                    || (!Methods.containsTypeVariableParameter(method) && !Types.containsTypeVariable(method.returnType()))) {
                resolvedMethodDescriptor = null;
            } else {
                List<Type> paramTypes = Types.getResolvedParameters(delegateTypeClass, resolvedTypeParameters, method,
                        index);
                Type returnType = Types.resolveTypeParam(method.returnType(), resolvedTypeParameters, index);
                String[] paramTypesArray = new String[paramTypes.size()];
                for (int i = 0; i < paramTypesArray.length; i++) {
                    paramTypesArray[i] = DescriptorUtils.typeToString(paramTypes.get(i));
                }
                resolvedMethodDescriptor = MethodDescriptor.ofMethod(method.declaringClass().toString(),
                        method.name(), DescriptorUtils.typeToString(returnType), paramTypesArray);
            }

            DecoratorMethod nextDecorator = null;
            MethodDescriptor nextDecoratorDecorated = null;
            for (Entry<MethodDescriptor, DecoratorMethod> e : nextDecorators.entrySet()) {
                // Find the next decorator for the current delegate type method
                if (Methods.descriptorMatches(e.getKey(), methodDescriptor)
                        || (resolvedMethodDescriptor != null
                                && Methods.descriptorMatches(e.getKey(), resolvedMethodDescriptor))
                        || Methods.descriptorMatches(MethodDescriptor.of(e.getValue().method), methodDescriptor)) {
                    nextDecorator = e.getValue();
                    nextDecoratorDecorated = e.getKey();
                    break;
                }
            }

            if (nextDecorator != null
                    && isDecorated(decoratedMethodDescriptors, methodDescriptor, resolvedMethodDescriptor,
                            nextDecoratorDecorated)) {
                // This method is decorated by this decorator and there is a next decorator in the chain
                // Just delegate to the next decorator
                delegateTo = forward.readInstanceField(nextDecoratorToField.get(nextDecorator.decorator), forward.getThis());
                if (delegateTypeIsInterface) {
                    ret = forward.invokeInterfaceMethod(methodDescriptor, delegateTo, params);
                } else {
                    MethodDescriptor virtualMethod = MethodDescriptor.ofMethod(providerTypeName,
                            methodDescriptor.getName(),
                            nextDecorator.method.returnType(),
                            nextDecorator.method.parameterTypes());
                    ret = forward.invokeVirtualMethod(virtualMethod, delegateTo, params);
                }

            } else {
                // This method is not decorated or no next decorator was found in the chain
                MethodDescriptor forwardingMethod = null;
                MethodInfo decoratedMethod = bean.getDecoratedMethod(m.method, decorator);
                MethodDescriptor decoratedMethodDescriptor = decoratedMethod != null ? MethodDescriptor.of(decoratedMethod)
                        : null;
                for (Entry<MethodDescriptor, MethodDescriptor> entry : forwardingMethods.entrySet()) {
                    if (Methods.descriptorMatches(entry.getKey(), methodDescriptor)
                            // Also try to find the forwarding method for the resolved variant
                            || (resolvedMethodDescriptor != null
                                    && Methods.descriptorMatches(entry.getKey(), resolvedMethodDescriptor))
                            // Finally, try to match the decorated method
                            || (decoratedMethodDescriptor != null
                                    && Methods.descriptorMatches(entry.getKey(), decoratedMethodDescriptor))) {
                        forwardingMethod = entry.getValue();
                        break;
                    }
                }
                if (forwardingMethod != null) {
                    // Delegate to the subclass forwarding method
                    delegateTo = forward.readInstanceField(subclassField.getFieldDescriptor(), forward.getThis());
                    ret = forward.invokeVirtualMethod(forwardingMethod, delegateTo, params);
                } else {
                    // No forwarding method exists
                    // Simply delegate to subclass
                    delegateTo = forward.readInstanceField(subclassField.getFieldDescriptor(), forward.getThis());
                    if (Modifier.isInterface(method.declaringClass().flags())) {
                        ret = forward.invokeInterfaceMethod(methodDescriptor, delegateTo, params);
                    } else {
                        ret = forward.invokeVirtualMethod(methodDescriptor, delegateTo, params);
                    }
                }
            }

            // Finally write the bytecode
            forward.returnValue(ret);
        }
        delegateSubclass.close();

        // Now modify the subclass constructor
        ResultHandle constructorMethodParam = subclassConstructor
                .getMethodParam(paramIndex);
        // create instance of each decorator -> InjectableDecorator.get()
        ResultHandle creationalContext = subclassConstructor.invokeStaticMethod(MethodDescriptors.CREATIONAL_CTX_CHILD,
                creationalContextHandle);

        // Create new delegate subclass instance and set the DecoratorDelegateProvider to satisfy the delegate IP
        ResultHandle[] paramHandles = new ResultHandle[constructorParameterTypes.size()];
        int paramIdx = 0;
        paramHandles[paramIdx++] = subclassConstructor.getThis();
        for (DecoratorInfo decoratorParameter : decoratorParameters) {
            ResultHandle decoratorHandle = decoratorToResultHandle.get(decoratorParameter.getIdentifier());
            if (decoratorHandle == null) {
                throw new IllegalStateException("Decorator handle must not be null");
            }
            paramHandles[paramIdx++] = decoratorHandle;
        }
        ResultHandle delegateSubclassInstance = subclassConstructor.newInstance(MethodDescriptor.ofConstructor(
                delegateSubclass.getClassName(), constructorParameterTypes.toArray(new String[0])), paramHandles);
        subclassConstructor.invokeStaticMethod(MethodDescriptors.DECORATOR_DELEGATE_PROVIDER_SET, delegateSubclassInstance);

        ResultHandle decoratorInstance = subclassConstructor.invokeInterfaceMethod(
                MethodDescriptors.INJECTABLE_REF_PROVIDER_GET, constructorMethodParam, creationalContext);
        // And unset the delegate IP afterwards
        subclassConstructor.invokeStaticMethod(MethodDescriptors.DECORATOR_DELEGATE_PROVIDER_UNSET);

        decoratorToResultHandle.put(decorator.getIdentifier(), decoratorInstance);

        // Store the decorator instance in a field
        FieldCreator decoratorField = subclass.getFieldCreator(decorator.getIdentifier(), Object.class)
                .setModifiers(ACC_PRIVATE | ACC_FINAL);
        subclassConstructor.writeInstanceField(decoratorField.getFieldDescriptor(), subclassConstructor.getThis(),
                decoratorInstance);
    }

    private boolean isDecorated(Set<MethodDescriptor> decoratedMethodDescriptors, MethodDescriptor original,
            MethodDescriptor resolved, MethodDescriptor nextDecoratorDecorated) {
        for (MethodDescriptor decorated : decoratedMethodDescriptors) {
            if (Methods.descriptorMatches(decorated, original)
                    || (resolved != null && Methods.descriptorMatches(decorated, resolved))
                    || Methods.descriptorMatches(decorated, nextDecoratorDecorated)) {
                return true;
            }
        }
        return false;
    }

    private MethodDescriptor createForwardingMethod(ClassCreator subclass, String providerTypeName, MethodInfo method) {
        MethodDescriptor methodDescriptor = MethodDescriptor.of(method);
        String forwardMethodName = method.name() + "$$superforward";
        MethodDescriptor forwardDescriptor = MethodDescriptor.ofMethod(subclass.getClassName(), forwardMethodName,
                methodDescriptor.getReturnType(),
                methodDescriptor.getParameterTypes());
        MethodCreator forward = subclass.getMethodCreator(forwardDescriptor);
        List<Type> parameters = method.parameterTypes();
        ResultHandle[] params = new ResultHandle[parameters.size()];
        for (int i = 0; i < parameters.size(); ++i) {
            params[i] = forward.getMethodParam(i);
        }
        MethodDescriptor virtualMethod = MethodDescriptor.ofMethod(providerTypeName, methodDescriptor.getName(),
                methodDescriptor.getReturnType(), methodDescriptor.getParameterTypes());
        forward.returnValue(forward.invokeSpecialMethod(virtualMethod, forward.getThis(), params));
        return forwardDescriptor;
    }

    private void createInterceptedMethod(BeanInfo bean, MethodInfo method, ClassCreator subclass,
            String providerTypeName, FieldDescriptor metadataField, FieldDescriptor constructedField,
            MethodDescriptor forwardMethod) {

        MethodDescriptor originalMethodDescriptor = MethodDescriptor.of(method);
        MethodCreator interceptedMethod = subclass.getMethodCreator(originalMethodDescriptor);

        // Params
        // Object[] params = new Object[] {p1}
        List<Type> parameters = method.parameterTypes();
        ResultHandle paramsHandle;
        if (parameters.isEmpty()) {
            paramsHandle = interceptedMethod.loadNull();
        } else {
            paramsHandle = interceptedMethod.newArray(Object.class,
                    interceptedMethod.load(parameters.size()));
            for (int i = 0; i < parameters.size(); i++) {
                interceptedMethod.writeArrayValue(paramsHandle, i, interceptedMethod.getMethodParam(i));
            }
        }

        // Delegate to super class if not constructed yet
        BytecodeCreator notConstructed = interceptedMethod
                .ifFalse(interceptedMethod.readInstanceField(constructedField, interceptedMethod.getThis())).trueBranch();
        ResultHandle[] params = new ResultHandle[parameters.size()];
        for (int i = 0; i < parameters.size(); ++i) {
            params[i] = notConstructed.getMethodParam(i);
        }
        if (Modifier.isAbstract(method.flags())) {
            notConstructed.throwException(IllegalStateException.class, "Cannot delegate to an abstract method");
        } else {
            notConstructed.returnValue(notConstructed.invokeVirtualMethod(forwardMethod, notConstructed.getThis(), params));
        }

        for (Type declaredException : method.exceptions()) {
            interceptedMethod.addException(declaredException.name().toString());
        }

        TryBlock tryCatch = interceptedMethod.tryBlock();
        // catch exceptions declared on the original method
        boolean addCatchRuntimeException = true;
        boolean addCatchException = true;
        Set<DotName> declaredExceptions = new LinkedHashSet<>(method.exceptions().size());
        for (Type declaredException : method.exceptions()) {
            declaredExceptions.add(declaredException.name());
        }
        for (DotName declaredException : declaredExceptions) {
            CatchBlockCreator catchDeclaredException = tryCatch.addCatch(declaredException.toString());
            catchDeclaredException.throwException(catchDeclaredException.getCaughtException());

            if (JAVA_LANG_RUNTIME_EXCEPTION.equals(declaredException) ||
                    JAVA_LANG_THROWABLE.equals(declaredException)) {
                addCatchRuntimeException = false;
            }
            if (JAVA_LANG_EXCEPTION.equals(declaredException) ||
                    JAVA_LANG_THROWABLE.equals(declaredException)) {
                addCatchException = false;
            }
        }
        // catch (RuntimeException e) if not already caught
        if (addCatchRuntimeException) {
            CatchBlockCreator catchRuntimeException = tryCatch.addCatch(RuntimeException.class);
            catchRuntimeException.throwException(catchRuntimeException.getCaughtException());
        }
        // now catch the rest (Exception e) if not already caught
        // this catch is _not_ included for Kotlin methods because Kotlin has not checked exceptions contract
        if (addCatchException && !isKotlinMethod(method)) {
            CatchBlockCreator catchOtherExceptions = tryCatch.addCatch(Exception.class);
            // and wrap them in a new RuntimeException(e)
            catchOtherExceptions.throwException(ArcUndeclaredThrowableException.class, "Error invoking subclass method",
                    catchOtherExceptions.getCaughtException());
        }
        // InvocationContexts.performAroundInvoke(...)
        ResultHandle methodMetadataHandle = tryCatch.readInstanceField(metadataField, tryCatch.getThis());
        ResultHandle ret = tryCatch.invokeStaticMethod(MethodDescriptors.INVOCATION_CONTEXTS_PERFORM_AROUND_INVOKE,
                tryCatch.getThis(), paramsHandle, methodMetadataHandle);
        tryCatch.returnValue(ret);
    }

    /**
     *
     * @param classOutput
     * @param bean
     * @param subclass
     * @param preDestroysField
     */
    protected void createDestroy(ClassOutput classOutput, BeanInfo bean, ClassCreator subclass,
            FieldDescriptor preDestroysField) {
        if (preDestroysField != null) {
            MethodCreator destroy = subclass.getMethodCreator(MethodDescriptor.ofMethod(subclass.getClassName(),
                    DESTROY_METHOD_NAME, void.class, Runnable.class));
            ResultHandle predestroysHandle = destroy.readInstanceField(preDestroysField, destroy.getThis());
            ResultHandle forward = destroy.getMethodParam(0);

            // Interceptor bindings
            InterceptionInfo preDestroy = bean.getLifecycleInterceptors(InterceptionType.PRE_DESTROY);
            ResultHandle bindingsArray = destroy.newArray(Object.class, preDestroy.bindings.size());
            int bindingsIndex = 0;
            for (AnnotationInstance binding : preDestroy.bindings) {
                // Create annotation literals first
                ClassInfo bindingClass = bean.getDeployment().getInterceptorBinding(binding.name());
                destroy.writeArrayValue(bindingsArray, bindingsIndex++,
                        annotationLiterals.create(destroy, bindingClass, binding));
            }

            // try
            TryBlock tryCatch = destroy.tryBlock();
            // catch (Exception e)
            CatchBlockCreator exception = tryCatch.addCatch(Exception.class);
            // throw new RuntimeException(e)
            exception.throwException(RuntimeException.class, "Error destroying subclass", exception.getCaughtException());

            // InvocationContextImpl.preDestroy(this,predestroys)
            ResultHandle invocationContext = tryCatch.invokeStaticMethod(MethodDescriptors.INVOCATION_CONTEXTS_PRE_DESTROY,
                    tryCatch.getThis(), predestroysHandle,
                    tryCatch.invokeStaticMethod(MethodDescriptors.SETS_OF, bindingsArray),
                    forward);

            // InvocationContext.proceed()
            tryCatch.invokeInterfaceMethod(MethodDescriptors.INVOCATION_CONTEXT_PROCEED, invocationContext);
            destroy.returnValue(null);
        }
    }

    private static class IntegerHolder {
        private int i = 1;
    }

}
