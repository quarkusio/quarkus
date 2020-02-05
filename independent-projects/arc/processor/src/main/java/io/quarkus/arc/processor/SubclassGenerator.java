package io.quarkus.arc.processor;

import static io.quarkus.arc.processor.IndexClassLookupUtils.getClassByName;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;

import io.quarkus.arc.ArcUndeclaredThrowableException;
import io.quarkus.arc.InjectableInterceptor;
import io.quarkus.arc.Subclass;
import io.quarkus.arc.impl.SubclassMethodMetadata;
import io.quarkus.arc.processor.BeanInfo.InterceptionInfo;
import io.quarkus.arc.processor.ResourceOutput.Resource;
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
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

/**
 *
 * @author Martin Kouba
 */
public class SubclassGenerator extends AbstractGenerator {

    private static final DotName JAVA_LANG_THROWABLE = DotNames.create(Throwable.class.getName());
    private static final DotName JAVA_LANG_EXCEPTION = DotNames.create(Exception.class.getName());
    private static final DotName JAVA_LANG_RUNTIME_EXCEPTION = DotNames.create(RuntimeException.class.getName());

    static final String SUBCLASS_SUFFIX = "_Subclass";
    static final String DESTROY_METHOD_NAME = "arc$destroy";

    protected static final String FIELD_NAME_PREDESTROYS = "preDestroys";
    protected static final String FIELD_NAME_METADATA = "metadata";
    protected static final FieldDescriptor FIELD_METADATA_METHOD = FieldDescriptor.of(SubclassMethodMetadata.class, "method",
            Method.class);
    protected static final FieldDescriptor FIELD_METADATA_CHAIN = FieldDescriptor.of(SubclassMethodMetadata.class, "chain",
            List.class);
    protected static final FieldDescriptor FIELD_METADATA_BINDINGS = FieldDescriptor.of(SubclassMethodMetadata.class,
            "bindings", Set.class);

    private final Predicate<DotName> applicationClassPredicate;

    static String generatedName(DotName providerTypeName, String baseName) {
        return DotNames.packageName(providerTypeName).replace('.', '/') + "/" + baseName + SUBCLASS_SUFFIX;
    }

    private final AnnotationLiteralProcessor annotationLiterals;

    /**
     *
     * @param annotationLiterals
     * @param applicationClassPredicate
     */
    public SubclassGenerator(AnnotationLiteralProcessor annotationLiterals, Predicate<DotName> applicationClassPredicate) {
        this.applicationClassPredicate = applicationClassPredicate;
        this.annotationLiterals = annotationLiterals;
    }

    /**
     *
     * @param bean
     * @param beanClassName Fully qualified class name
     * @return a java file
     */
    Collection<Resource> generate(BeanInfo bean, String beanClassName, ReflectionRegistration reflectionRegistration) {

        ResourceClassOutput classOutput = new ResourceClassOutput(applicationClassPredicate.test(bean.getBeanClass()));

        Type providerType = bean.getProviderType();
        ClassInfo providerClass = getClassByName(bean.getDeployment().getIndex(), providerType.name());
        String providerTypeName = providerClass.name().toString();
        String baseName = getBaseName(bean, beanClassName);
        String generatedName = generatedName(providerType.name(), baseName);

        // Foo_Subclass extends Foo implements Subclass
        ClassCreator subclass = ClassCreator.builder().classOutput(classOutput).className(generatedName)
                .superClass(providerTypeName).interfaces(Subclass.class)
                .build();

        FieldDescriptor preDestroyField = createConstructor(classOutput, bean, subclass, providerTypeName,
                reflectionRegistration);
        createDestroy(classOutput, bean, subclass, preDestroyField);

        subclass.close();
        return classOutput.getResources();
    }

    protected FieldDescriptor createConstructor(ClassOutput classOutput, BeanInfo bean, ClassCreator subclass,
            String providerTypeName,
            ReflectionRegistration reflectionRegistration) {

        // Constructor parameters
        List<String> parameterTypes = new ArrayList<>();
        // First constructor injection points
        Optional<Injection> constructorInjection = bean.getConstructorInjection();
        if (constructorInjection.isPresent()) {
            for (InjectionPointInfo injectionPoint : constructorInjection.get().injectionPoints) {
                parameterTypes.add(injectionPoint.getRequiredType().name().toString());
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
        // private final Map<String,SubclassMethodMetadata> metadata
        // metadata = new HashMap<>()
        int metadataMapCapacity = bean.getInterceptedMethods().size();
        if (metadataMapCapacity < 3) {
            metadataMapCapacity++;
        } else {
            metadataMapCapacity = (int) ((float) metadataMapCapacity / 0.75F + 1.0F);
        }
        FieldCreator metadataField = subclass.getFieldCreator(FIELD_NAME_METADATA, Map.class.getName())
                .setModifiers(ACC_PRIVATE | ACC_FINAL);
        ResultHandle metadataHandle = constructor.newInstance(MethodDescriptor.ofConstructor(HashMap.class, int.class),
                constructor.load(metadataMapCapacity));
        constructor.writeInstanceField(metadataField.getFieldDescriptor(), constructor.getThis(),
                metadataHandle);

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

        int methodIdx = 1;
        for (Entry<MethodInfo, InterceptionInfo> entry : bean.getInterceptedMethods().entrySet()) {
            String methodId = "m" + methodIdx++;
            MethodInfo method = entry.getKey();
            ResultHandle methodIdHandle = constructor.load(methodId);
            InterceptionInfo interceptedMethod = entry.getValue();

            // 1. Interceptor chain
            ResultHandle chainHandle = interceptorChains.computeIfAbsent(interceptedMethod.interceptors, interceptorChainsFun);

            // 2. Method method = Reflections.findMethod(org.jboss.weld.arc.test.interceptors.SimpleBean.class,"foo",java.lang.String.class)
            ResultHandle[] paramsHandles = new ResultHandle[3];
            paramsHandles[0] = constructor.loadClass(providerTypeName);
            paramsHandles[1] = constructor.load(method.name());
            if (!method.parameters().isEmpty()) {
                ResultHandle paramsArray = constructor.newArray(Class.class, constructor.load(method.parameters().size()));
                for (ListIterator<Type> iterator = method.parameters().listIterator(); iterator.hasNext();) {
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
                    interceptedMethod.bindings.stream().map(BindingKey::new).collect(Collectors.toList()), bindingsFun);

            //Now create SubclassMethodMetadata for the given intercepted method
            ResultHandle methodMetadataHandle = constructor.newInstance(MethodDescriptors.SUBCLASS_METHOD_METADATA_CONSTRUCTOR,
                    chainHandle, methodHandle, bindingsHandle);
            // metadata.put("m1", new SubclassMethodMetadata(...))
            constructor.invokeInterfaceMethod(MethodDescriptors.MAP_PUT, metadataHandle, methodIdHandle, methodMetadataHandle);

            // Needed when running on native image
            reflectionRegistration.registerMethod(method);

            // Finally create the forwarding method
            createForwardingMethod(classOutput, bean, method, methodId, subclass, providerTypeName,
                    metadataField.getFieldDescriptor(),
                    interceptedMethod);
        }

        constructor.returnValue(null);
        return preDestroysField != null ? preDestroysField.getFieldDescriptor() : null;
    }

    private void createForwardingMethod(ClassOutput classOutput, BeanInfo bean, MethodInfo method, String methodId,
            ClassCreator subclass,
            String providerTypeName, FieldDescriptor metadataField,
            InterceptionInfo interceptedMethod) {

        MethodDescriptor originalMethodDescriptor = MethodDescriptor.of(method);
        MethodCreator forwardMethod = subclass.getMethodCreator(originalMethodDescriptor);

        // Params
        // Object[] params = new Object[] {p1}
        ResultHandle paramsHandle = forwardMethod.newArray(Object.class, forwardMethod.load(method.parameters().size()));
        for (int i = 0; i < method.parameters().size(); i++) {
            forwardMethod.writeArrayValue(paramsHandle, i, forwardMethod.getMethodParam(i));
        }

        // if(!this.bean == null) return super.foo()
        BytecodeCreator notConstructed = forwardMethod
                .ifNull(forwardMethod.readInstanceField(metadataField, forwardMethod.getThis())).trueBranch();
        ResultHandle[] params = new ResultHandle[method.parameters().size()];
        for (int i = 0; i < method.parameters().size(); ++i) {
            params[i] = notConstructed.getMethodParam(i);
        }
        if (Modifier.isAbstract(method.flags())) {
            notConstructed.throwException(IllegalStateException.class, "Cannot delegate to an abstract method");
        } else {
            MethodDescriptor superDescriptor = MethodDescriptor.ofMethod(providerTypeName, method.name(),
                    method.returnType().name().toString(),
                    method.parameters().stream().map(p -> p.name().toString()).toArray());
            notConstructed.returnValue(
                    notConstructed.invokeSpecialMethod(superDescriptor, notConstructed.getThis(), params));
        }

        // Forwarding function
        // Function<InvocationContext, Object> forward = ctx -> super.foo((java.lang.String)ctx.getParameters()[0])
        FunctionCreator func = forwardMethod.createFunction(Function.class);
        BytecodeCreator funcBytecode = func.getBytecode();
        ResultHandle ctxHandle = funcBytecode.getMethodParam(0);
        ResultHandle[] superParamHandles = new ResultHandle[method.parameters().size()];
        ResultHandle ctxParamsHandle = funcBytecode.invokeInterfaceMethod(
                MethodDescriptor.ofMethod(InvocationContext.class, "getParameters", Object[].class),
                ctxHandle);
        // autoboxing is handled inside Gizmo
        for (int i = 0; i < superParamHandles.length; i++) {
            superParamHandles[i] = funcBytecode.readArrayValue(ctxParamsHandle, i);
        }
        ResultHandle superResult = funcBytecode.invokeSpecialMethod(
                MethodDescriptor.ofMethod(providerTypeName, method.name(), method.returnType().name().toString(),
                        method.parameters().stream().map(p -> p.name().toString()).collect(Collectors.toList())
                                .toArray(new String[0])),
                forwardMethod.getThis(), superParamHandles);
        funcBytecode.returnValue(superResult != null ? superResult : funcBytecode.loadNull());
        for (Type declaredException : method.exceptions()) {
            forwardMethod.addException(declaredException.name().toString());
        }

        TryBlock tryCatch = forwardMethod.tryBlock();
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
        ResultHandle methodIdHandle = tryCatch.load(methodId);
        ResultHandle methodMetadataHandle = tryCatch.invokeInterfaceMethod(MethodDescriptors.MAP_GET,
                tryCatch.readInstanceField(metadataField, tryCatch.getThis()), methodIdHandle);
        ResultHandle ret = tryCatch.invokeStaticMethod(MethodDescriptors.INVOCATION_CONTEXTS_PERFORM_AROUND_INVOKE,
                tryCatch.getThis(),
                tryCatch.readInstanceField(FIELD_METADATA_METHOD, methodMetadataHandle), func.getInstance(), paramsHandle,
                tryCatch.readInstanceField(FIELD_METADATA_CHAIN, methodMetadataHandle),
                tryCatch.readInstanceField(FIELD_METADATA_BINDINGS, methodMetadataHandle));
        tryCatch.returnValue(superResult != null ? ret : null);
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
