package io.quarkus.arc.processor;

import static io.quarkus.arc.processor.IndexClassLookupUtils.getClassByName;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;

import io.quarkus.arc.ArcUndeclaredThrowableException;
import io.quarkus.arc.InjectableDecorator;
import io.quarkus.arc.InjectableInterceptor;
import io.quarkus.arc.Subclass;
import io.quarkus.arc.impl.InterceptedMethodMetadata;
import io.quarkus.arc.processor.BeanInfo.DecorationInfo;
import io.quarkus.arc.processor.BeanInfo.InterceptionInfo;
import io.quarkus.arc.processor.ResourceOutput.Resource;
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
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.InterceptionType;
import javax.interceptor.InvocationContext;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.jandex.Type.Kind;
import org.jboss.jandex.TypeVariable;

/**
 * A subclass is generated for any intercepted/decorated bean.
 */
public class SubclassGenerator extends AbstractGenerator {

    private static final DotName JAVA_LANG_THROWABLE = DotNames.create(Throwable.class.getName());
    private static final DotName JAVA_LANG_EXCEPTION = DotNames.create(Exception.class.getName());
    private static final DotName JAVA_LANG_RUNTIME_EXCEPTION = DotNames.create(RuntimeException.class.getName());

    static final String SUBCLASS_SUFFIX = "_Subclass";
    static final String DESTROY_METHOD_NAME = "arc$destroy";

    protected static final String FIELD_NAME_PREDESTROYS = "arc$preDestroys";
    protected static final String FIELD_NAME_CONSTRUCTED = "arc$constructed";
    protected static final FieldDescriptor FIELD_METADATA_METHOD = FieldDescriptor.of(InterceptedMethodMetadata.class, "method",
            Method.class);
    protected static final FieldDescriptor FIELD_METADATA_CHAIN = FieldDescriptor.of(InterceptedMethodMetadata.class, "chain",
            List.class);
    protected static final FieldDescriptor FIELD_METADATA_BINDINGS = FieldDescriptor.of(InterceptedMethodMetadata.class,
            "bindings", Set.class);

    private final Predicate<DotName> applicationClassPredicate;
    private final ReflectionRegistration reflectionRegistration;
    private final Set<String> existingClasses;

    static String generatedName(DotName providerTypeName, String baseName) {
        String packageName = DotNames.internalPackageNameWithTrailingSlash(providerTypeName);
        return packageName + baseName + SUBCLASS_SUFFIX;
    }

    private final AnnotationLiteralProcessor annotationLiterals;

    public SubclassGenerator(AnnotationLiteralProcessor annotationLiterals, Predicate<DotName> applicationClassPredicate,
            boolean generateSources, ReflectionRegistration reflectionRegistration,
            Set<String> existingClasses) {
        super(generateSources);
        this.applicationClassPredicate = applicationClassPredicate;
        this.annotationLiterals = annotationLiterals;
        this.reflectionRegistration = reflectionRegistration;
        this.existingClasses = existingClasses;
    }

    Collection<Resource> generate(BeanInfo bean, String beanClassName) {

        ResourceClassOutput classOutput = new ResourceClassOutput(applicationClassPredicate.test(bean.getBeanClass()),
                generateSources);

        Type providerType = bean.getProviderType();
        ClassInfo providerClass = getClassByName(bean.getDeployment().getBeanArchiveIndex(), providerType.name());
        String providerTypeName = providerClass.name().toString();
        String baseName = getBaseName(bean, beanClassName);
        String generatedName = generatedName(providerType.name(), baseName);
        if (existingClasses.contains(generatedName)) {
            return Collections.emptyList();
        }

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
        int methodIdx = 1;
        for (MethodInfo method : interceptedOrDecoratedMethods) {
            forwardingMethods.put(MethodDescriptor.of(method),
                    createForwardingMethod(subclass, providerTypeName, method, methodIdx));
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

        // Init intercepted methods and interceptor chains
        FieldCreator constructedField = subclass.getFieldCreator(FIELD_NAME_CONSTRUCTED, boolean.class)
                .setModifiers(ACC_PRIVATE | ACC_FINAL);

        // Shared interceptor bindings literals
        Map<BindingKey, ResultHandle> bindingsLiterals = new HashMap<>();
        Function<BindingKey, ResultHandle> bindingsLiteralFun = new Function<SubclassGenerator.BindingKey, ResultHandle>() {
            @Override
            public ResultHandle apply(BindingKey key) {
                // Create annotation literal if needed
                ClassInfo bindingClass = bean.getDeployment()
                        .getInterceptorBinding(key.annotation.name());
                return annotationLiterals.process(constructor, classOutput, bindingClass, key.annotation,
                        Types.getPackageName(subclass.getClassName()));
            }
        };
        // Shared lists of interceptor bindings literals
        Map<List<BindingKey>, ResultHandle> bindings = new HashMap<>();
        Function<List<BindingKey>, ResultHandle> bindingsFun = new Function<List<BindingKey>, ResultHandle>() {
            @Override
            public ResultHandle apply(List<BindingKey> keys) {
                if (keys.size() == 1) {
                    return constructor.invokeStaticMethod(MethodDescriptors.COLLECTIONS_SINGLETON,
                            bindingsLiterals.computeIfAbsent(keys.iterator().next(), bindingsLiteralFun));
                } else {
                    ResultHandle bindingsHandle = constructor.newInstance(MethodDescriptor.ofConstructor(HashSet.class));
                    for (BindingKey binding : keys) {
                        constructor.invokeInterfaceMethod(MethodDescriptors.SET_ADD, bindingsHandle,
                                bindingsLiterals.computeIfAbsent(binding, bindingsLiteralFun));
                    }
                    return bindingsHandle;
                }
            }
        };
        // Shared interceptor chains
        Map<List<InterceptorInfo>, ResultHandle> interceptorChains = new HashMap<>();
        Function<List<InterceptorInfo>, ResultHandle> interceptorChainsFun = new Function<List<InterceptorInfo>, ResultHandle>() {
            @Override
            public ResultHandle apply(List<InterceptorInfo> interceptors) {
                if (interceptors.size() == 1) {
                    // List<InvocationContextImpl.InterceptorInvocation> m1Chain = Collections.singletonList(...);
                    InterceptorInfo interceptor = interceptors.get(0);
                    ResultHandle interceptorInstance = interceptorInstanceToResultHandle.get(interceptor.getIdentifier());
                    ResultHandle interceptionInvocation = constructor.invokeStaticMethod(
                            MethodDescriptors.INTERCEPTOR_INVOCATION_AROUND_INVOKE,
                            interceptorToResultHandle.get(interceptor.getIdentifier()), interceptorInstance);
                    return constructor.invokeStaticMethod(MethodDescriptors.COLLECTIONS_SINGLETON_LIST,
                            interceptionInvocation);
                } else {
                    // List<InvocationContextImpl.InterceptorInvocation> m1Chain = new ArrayList<>();
                    ResultHandle chainHandle = constructor.newInstance(MethodDescriptor.ofConstructor(ArrayList.class));
                    for (InterceptorInfo interceptor : interceptors) {
                        // m1Chain.add(InvocationContextImpl.InterceptorInvocation.aroundInvoke(p3,interceptorInstanceMap.get(InjectableInterceptor.getIdentifier())))
                        ResultHandle interceptorInstance = interceptorInstanceToResultHandle.get(interceptor.getIdentifier());
                        ResultHandle interceptionInvocation = constructor.invokeStaticMethod(
                                MethodDescriptors.INTERCEPTOR_INVOCATION_AROUND_INVOKE,
                                interceptorToResultHandle.get(interceptor.getIdentifier()), interceptorInstance);
                        constructor.invokeInterfaceMethod(MethodDescriptors.LIST_ADD, chainHandle, interceptionInvocation);
                    }
                    return chainHandle;
                }
            }
        };

        methodIdx = 1;
        for (MethodInfo method : interceptedOrDecoratedMethods) {
            MethodDescriptor methodDescriptor = MethodDescriptor.of(method);
            InterceptionInfo interception = bean.getInterceptedMethods().get(method);
            DecorationInfo decoration = bean.getDecoratedMethods().get(method);
            MethodDescriptor forwardDescriptor = forwardingMethods.get(methodDescriptor);
            List<Type> parameters = method.parameters();

            if (interception != null) {
                // Each intercepted method has a corresponding InterceptedMethodMetadata field
                FieldCreator metadataField = subclass
                        .getFieldCreator("arc$" + methodIdx++, InterceptedMethodMetadata.class.getName())
                        .setModifiers(ACC_PRIVATE | ACC_FINAL);

                // 1. Interceptor chain
                ResultHandle chainHandle = interceptorChains.computeIfAbsent(interception.interceptors, interceptorChainsFun);

                // 2. Method method = Reflections.findMethod(org.jboss.weld.arc.test.interceptors.SimpleBean.class,"foo",java.lang.String.class)
                ResultHandle[] paramsHandles = new ResultHandle[3];
                paramsHandles[0] = constructor.loadClass(providerTypeName);
                paramsHandles[1] = constructor.load(method.name());
                if (!parameters.isEmpty()) {
                    ResultHandle paramsArray = constructor.newArray(Class.class, constructor.load(parameters.size()));
                    for (ListIterator<Type> iterator = parameters.listIterator(); iterator.hasNext();) {
                        constructor.writeArrayValue(paramsArray, iterator.nextIndex(),
                                constructor.loadClass(iterator.next().name().toString()));
                    }
                    paramsHandles[2] = paramsArray;
                } else {
                    paramsHandles[2] = constructor.readStaticField(FieldDescriptors.ANNOTATION_LITERALS_EMPTY_CLASS_ARRAY);
                }
                ResultHandle methodHandle = constructor.invokeStaticMethod(MethodDescriptors.REFLECTIONS_FIND_METHOD,
                        paramsHandles);

                // 3. Interceptor bindings
                // Note that we use a shared list if possible
                ResultHandle bindingsHandle = bindings.computeIfAbsent(
                        interception.bindings.stream().map(BindingKey::new).collect(Collectors.toList()), bindingsFun);

                // Now create metadata for the given intercepted method
                ResultHandle methodMetadataHandle = constructor.newInstance(
                        MethodDescriptors.INTERCEPTED_METHOD_METADATA_CONSTRUCTOR,
                        chainHandle, methodHandle, bindingsHandle);

                constructor.writeInstanceField(metadataField.getFieldDescriptor(), constructor.getThis(), methodMetadataHandle);

                // Needed when running on native image
                reflectionRegistration.registerMethod(method);

                // Finally create the intercepted method
                createInterceptedMethod(classOutput, bean, method, subclass, providerTypeName,
                        metadataField.getFieldDescriptor(), constructedField.getFieldDescriptor(), forwardDescriptor,
                        decoration != null ? decoration.decorators.get(0) : null);
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

                DecoratorInfo firstDecorator = decoration.decorators.get(0);
                ResultHandle decoratorInstance = decoratedMethod.readInstanceField(FieldDescriptor.of(subclass.getClassName(),
                        firstDecorator.getIdentifier(), Object.class.getName()), decoratedMethod.getThis());

                String declaringClass = firstDecorator.getBeanClass().toString();
                if (firstDecorator.isAbstract()) {
                    String baseName = DecoratorGenerator.createBaseName(firstDecorator.getTarget().get().asClass());
                    String targetPackage = DotNames.packageName(firstDecorator.getProviderType().name());
                    declaringClass = generatedNameFromTarget(targetPackage, baseName, DecoratorGenerator.ABSTRACT_IMPL_SUFFIX);
                }
                MethodDescriptor virtualMethodDescriptor = MethodDescriptor.ofMethod(
                        declaringClass, methodDescriptor.getName(),
                        methodDescriptor.getReturnType(), methodDescriptor.getParameterTypes());
                decoratedMethod
                        .returnValue(decoratedMethod.invokeVirtualMethod(virtualMethodDescriptor, decoratorInstance, params));
            }
        }

        constructor.writeInstanceField(constructedField.getFieldDescriptor(), constructor.getThis(), constructor.load(true));

        constructor.returnValue(null);
        return preDestroysField != null ? preDestroysField.getFieldDescriptor() : null;
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

        Map<MethodDescriptor, DecoratorInfo> nextDecorators = bean.getNextDecorators(decorator);
        List<DecoratorInfo> decoratorParameters = new ArrayList<>(nextDecorators.values());
        Collections.sort(decoratorParameters);
        Set<MethodInfo> decoratedMethods = bean.getDecoratedMethods(decorator);
        Set<MethodDescriptor> decoratedMethodDescriptors = new HashSet<>(decoratedMethods.size());
        for (MethodInfo m : decoratedMethods) {
            decoratedMethodDescriptors.add(MethodDescriptor.of(m));
        }

        List<String> constructorParameterTypes = new ArrayList<>();
        // Fields and constructor
        FieldCreator subclassField = null;
        if (decoratedMethods.size() != decoratorParameters.size()) {
            subclassField = delegateSubclass.getFieldCreator("subclass", subclass.getClassName())
                    .setModifiers(ACC_PRIVATE | ACC_FINAL);
            constructorParameterTypes.add(subclass.getClassName());
        }
        Map<DecoratorInfo, FieldDescriptor> nextDecoratorToField = new HashMap<>();
        for (DecoratorInfo nextDecorator : decoratorParameters) {
            FieldCreator nextDecoratorField = delegateSubclass
                    .getFieldCreator(nextDecorator.getIdentifier(), nextDecorator.getBeanClass().toString())
                    .setModifiers(ACC_PRIVATE | ACC_FINAL);
            constructorParameterTypes.add(nextDecorator.getBeanClass().toString());
            nextDecoratorToField.put(nextDecorator, nextDecoratorField.getFieldDescriptor());
        }

        MethodCreator constructor = delegateSubclass.getMethodCreator(Methods.INIT, "V",
                constructorParameterTypes.toArray(new String[0]));
        int param = 0;
        // Invoke super()
        constructor.invokeSpecialMethod(MethodDescriptors.OBJECT_CONSTRUCTOR, constructor.getThis());
        // Set fields
        if (subclassField != null) {
            constructor.writeInstanceField(
                    subclassField.getFieldDescriptor(), constructor.getThis(), constructor.getMethodParam(param++));
        }
        for (FieldDescriptor field : nextDecoratorToField.values()) {
            constructor.writeInstanceField(
                    field, constructor.getThis(), constructor.getMethodParam(param++));
        }
        constructor.returnValue(null);

        IndexView index = bean.getDeployment().getBeanArchiveIndex();

        // Identify the set of methods that should be delegated
        // Note that the delegate subclass must override ALL methods from the delegate type
        // This is not enough if the delegate type is parameterized 
        List<MethodInfo> methods = new ArrayList<>();
        Methods.addDelegateTypeMethods(index, delegateTypeClass, methods);

        // The delegate type can declare type parameters
        // For example @Delegate Converter<String> should result in a T -> String mapping
        List<TypeVariable> typeParameters = delegateTypeClass.typeParameters();
        Map<TypeVariable, Type> resolvedTypeParameters = Collections.emptyMap();
        if (!typeParameters.isEmpty()) {
            resolvedTypeParameters = new HashMap<>();
            // The delegate type can be used to infer the parameter types
            Type delegateType = decorator.getDelegateType();
            if (delegateType.kind() == Kind.PARAMETERIZED_TYPE) {
                List<Type> typeArguments = delegateType.asParameterizedType().arguments();
                for (int i = 0; i < typeParameters.size(); i++) {
                    resolvedTypeParameters.put(typeParameters.get(i), typeArguments.get(i));
                }
            }
        }

        for (MethodInfo method : methods) {
            if (Methods.skipForDelegateSubclass(method)) {
                continue;
            }

            MethodDescriptor methodDescriptor = MethodDescriptor.of(method);
            MethodCreator forward = delegateSubclass.getMethodCreator(methodDescriptor);
            // Exceptions
            for (Type exception : method.exceptions()) {
                forward.addException(exception.toString());
            }

            ResultHandle ret = null;
            ResultHandle delegateTo;

            // Method params
            List<Type> parameters = method.parameters();
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

            DecoratorInfo nextDecorator = null;
            for (Entry<MethodDescriptor, DecoratorInfo> entry : nextDecorators.entrySet()) {
                if (Methods.descriptorMatches(entry.getKey(), methodDescriptor) || (resolvedMethodDescriptor != null
                        && Methods.descriptorMatches(entry.getKey(), resolvedMethodDescriptor))) {
                    nextDecorator = entry.getValue();
                    break;
                }
            }

            if (nextDecorator != null && isDecorated(decoratedMethodDescriptors, methodDescriptor, resolvedMethodDescriptor)) {
                // This method is decorated by this decorator and there is a next decorator in the chain
                // Just delegate to the next decorator
                delegateTo = forward.readInstanceField(nextDecoratorToField.get(nextDecorator), forward.getThis());
                if (delegateTypeIsInterface) {
                    ret = forward.invokeInterfaceMethod(methodDescriptor, delegateTo, params);
                } else {
                    MethodDescriptor virtualMethod = MethodDescriptor.ofMethod(providerTypeName,
                            methodDescriptor.getName(),
                            methodDescriptor.getReturnType(),
                            methodDescriptor.getParameterTypes());
                    ret = forward.invokeVirtualMethod(virtualMethod, delegateTo, params);
                }

            } else {
                // This method is not decorated or no next decorator was found in the chain
                MethodDescriptor forwardingMethod = null;
                for (Entry<MethodDescriptor, MethodDescriptor> entry : forwardingMethods.entrySet()) {
                    // Also try to find the forwarding method for the resolved variant
                    if (Methods.descriptorMatches(entry.getKey(), methodDescriptor) || (resolvedMethodDescriptor != null
                            && Methods.descriptorMatches(entry.getKey(), resolvedMethodDescriptor))) {
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
        if (subclassField != null) {
            paramHandles[paramIdx++] = subclassConstructor.getThis();
        }
        for (DecoratorInfo decoratorParameter : decoratorParameters) {
            paramHandles[paramIdx++] = decoratorToResultHandle.get(decoratorParameter.getIdentifier());
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
            MethodDescriptor resolved) {
        for (MethodDescriptor decorated : decoratedMethodDescriptors) {
            if (Methods.descriptorMatches(decorated, original)) {
                return true;
            }
        }
        if (resolved != null) {
            for (MethodDescriptor decorated : decoratedMethodDescriptors) {
                if (Methods.descriptorMatches(decorated, resolved)) {
                    return true;
                }
            }
        }
        return false;
    }

    private MethodDescriptor createForwardingMethod(ClassCreator subclass, String providerTypeName, MethodInfo method,
            int index) {
        MethodDescriptor methodDescriptor = MethodDescriptor.of(method);
        String forwardMethodName = method.name() + "$$superforward" + index;
        MethodDescriptor forwardDescriptor = MethodDescriptor.ofMethod(subclass.getClassName(), forwardMethodName,
                methodDescriptor.getReturnType(),
                methodDescriptor.getParameterTypes());
        MethodCreator forward = subclass.getMethodCreator(forwardDescriptor);
        List<Type> parameters = method.parameters();
        ResultHandle[] params = new ResultHandle[parameters.size()];
        for (int i = 0; i < parameters.size(); ++i) {
            params[i] = forward.getMethodParam(i);
        }
        MethodDescriptor virtualMethod = MethodDescriptor.ofMethod(providerTypeName, methodDescriptor.getName(),
                methodDescriptor.getReturnType(), methodDescriptor.getParameterTypes());
        forward.returnValue(forward.invokeSpecialMethod(virtualMethod, forward.getThis(), params));
        return forwardDescriptor;
    }

    private void createInterceptedMethod(ClassOutput classOutput, BeanInfo bean, MethodInfo method, ClassCreator subclass,
            String providerTypeName, FieldDescriptor metadataField, FieldDescriptor constructedField,
            MethodDescriptor forwardMethod, DecoratorInfo decorator) {

        MethodDescriptor originalMethodDescriptor = MethodDescriptor.of(method);
        MethodCreator interceptedMethod = subclass.getMethodCreator(originalMethodDescriptor);

        // Params
        // Object[] params = new Object[] {p1}
        List<Type> parameters = method.parameters();
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

        ResultHandle decoratorHandle = null;
        if (decorator != null) {
            decoratorHandle = interceptedMethod.readInstanceField(FieldDescriptor.of(subclass.getClassName(),
                    decorator.getIdentifier(), Object.class.getName()), interceptedMethod.getThis());
        }

        // Forwarding function
        // Function<InvocationContext, Object> forward = ctx -> super.foo((java.lang.String)ctx.getParameters()[0])
        FunctionCreator func = interceptedMethod.createFunction(Function.class);
        BytecodeCreator funcBytecode = func.getBytecode();
        ResultHandle ctxHandle = funcBytecode.getMethodParam(0);
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
        if (decorator != null) {
            AssignableResultHandle funDecoratorInstance = funcBytecode.createVariable(Object.class);
            funcBytecode.assign(funDecoratorInstance, decoratorHandle);
            String declaringClass = decorator.getBeanClass().toString();
            if (decorator.isAbstract()) {
                String baseName = DecoratorGenerator.createBaseName(decorator.getTarget().get().asClass());
                String targetPackage = DotNames.packageName(decorator.getProviderType().name());
                declaringClass = generatedNameFromTarget(targetPackage, baseName, DecoratorGenerator.ABSTRACT_IMPL_SUFFIX);
            }
            MethodDescriptor methodDescriptor = MethodDescriptor.ofMethod(
                    declaringClass, originalMethodDescriptor.getName(),
                    originalMethodDescriptor.getReturnType(), originalMethodDescriptor.getParameterTypes());
            funcBytecode
                    .returnValue(funcBytecode.invokeVirtualMethod(methodDescriptor, funDecoratorInstance, superParamHandles));

        } else {
            ResultHandle superResult = funcBytecode.invokeVirtualMethod(forwardMethod, interceptedMethod.getThis(),
                    superParamHandles);
            funcBytecode.returnValue(superResult != null ? superResult : funcBytecode.loadNull());
        }

        for (Type declaredException : method.exceptions()) {
            interceptedMethod.addException(declaredException.name().toString());
        }

        TryBlock tryCatch = interceptedMethod.tryBlock();
        // catch exceptions declared on the original method
        boolean addCatchRuntimeException = true;
        boolean addCatchException = true;
        for (Type declaredException : method.exceptions()) {
            CatchBlockCreator catchDeclaredException = tryCatch.addCatch(declaredException.name().toString());
            catchDeclaredException.throwException(catchDeclaredException.getCaughtException());

            if (JAVA_LANG_RUNTIME_EXCEPTION.equals(declaredException.name()) ||
                    JAVA_LANG_THROWABLE.equals(declaredException.name())) {
                addCatchRuntimeException = false;
            }
            if (JAVA_LANG_EXCEPTION.equals(declaredException.name()) ||
                    JAVA_LANG_THROWABLE.equals(declaredException.name())) {
                addCatchException = false;
            }
        }
        // catch (RuntimeException e) if not already caught
        if (addCatchRuntimeException) {
            CatchBlockCreator catchRuntimeException = tryCatch.addCatch(RuntimeException.class);
            catchRuntimeException.throwException(catchRuntimeException.getCaughtException());
        }
        // now catch the rest (Exception e) if not already caught
        if (addCatchException) {
            CatchBlockCreator catchOtherExceptions = tryCatch.addCatch(Exception.class);
            // and wrap them in a new RuntimeException(e)
            catchOtherExceptions.throwException(ArcUndeclaredThrowableException.class, "Error invoking subclass method",
                    catchOtherExceptions.getCaughtException());
        }
        // InvocationContexts.performAroundInvoke(...)
        ResultHandle methodMetadataHandle = tryCatch.readInstanceField(metadataField, tryCatch.getThis());
        ResultHandle ret = tryCatch.invokeStaticMethod(MethodDescriptors.INVOCATION_CONTEXTS_PERFORM_AROUND_INVOKE,
                tryCatch.getThis(),
                tryCatch.readInstanceField(FIELD_METADATA_METHOD, methodMetadataHandle), func.getInstance(), paramsHandle,
                tryCatch.readInstanceField(FIELD_METADATA_CHAIN, methodMetadataHandle),
                tryCatch.readInstanceField(FIELD_METADATA_BINDINGS, methodMetadataHandle));
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
            MethodCreator destroy = subclass
                    .getMethodCreator(MethodDescriptor.ofMethod(subclass.getClassName(), DESTROY_METHOD_NAME, void.class));
            ResultHandle predestroysHandle = destroy.readInstanceField(preDestroysField, destroy.getThis());

            // Interceptor bindings
            ResultHandle bindingsHandle = destroy.newInstance(MethodDescriptor.ofConstructor(HashSet.class));
            for (AnnotationInstance binding : bean.getLifecycleInterceptors(InterceptionType.PRE_DESTROY).bindings) {
                // Create annotation literals first
                ClassInfo bindingClass = bean.getDeployment().getInterceptorBinding(binding.name());
                destroy.invokeInterfaceMethod(MethodDescriptors.SET_ADD, bindingsHandle,
                        annotationLiterals.process(destroy, classOutput, bindingClass, binding,
                                Types.getPackageName(subclass.getClassName())));
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
                    bindingsHandle);

            // InvocationContext.proceed()
            tryCatch.invokeInterfaceMethod(MethodDescriptors.INVOCATION_CONTEXT_PROCEED, invocationContext);
            destroy.returnValue(null);
        }
    }

    /**
     * We cannot use {@link AnnotationInstance#equals(Object)} and {@link AnnotationInstance#hashCode()} because it includes the
     * annotation target.
     */
    static class BindingKey {

        final AnnotationInstance annotation;

        public BindingKey(AnnotationInstance annotation) {
            this.annotation = Objects.requireNonNull(annotation);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            BindingKey key = (BindingKey) o;
            return annotation.name().equals(key.annotation.name()) && annotation.values().equals(key.annotation.values());
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + annotation.name().hashCode();
            result = prime * result + annotation.values().hashCode();
            return result;
        }

    }

}
