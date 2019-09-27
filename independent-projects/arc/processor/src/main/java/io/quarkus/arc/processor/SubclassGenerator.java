package io.quarkus.arc.processor;

import static io.quarkus.arc.processor.IndexClassLookupUtils.getClassByName;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;

import io.quarkus.arc.ArcUndeclaredThrowableException;
import io.quarkus.arc.InjectableInterceptor;
import io.quarkus.arc.InvocationContextImpl.InterceptorInvocation;
import io.quarkus.arc.Subclass;
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
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
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

        // we build a map for each interceptor instance created, so that they are shared
        // Map<String, Object> where InjectableInterceptor.getIdentifier() is key and Object is instance of the interceptor for this bean
        ResultHandle interceptorInstanceMap = constructor.newInstance(MethodDescriptor.ofConstructor(HashMap.class));
        // build a map that links InterceptorInfo to ResultHandle
        Map<InterceptorInfo, ResultHandle> interceptorToResultHandle = new HashMap<>();
        for (int j = 0; j < boundInterceptors.size(); j++) {
            ResultHandle constructorMethodParam = constructor.getMethodParam(j + superParamsSize + 1);
            interceptorToResultHandle.put(boundInterceptors.get(j), constructorMethodParam);

            // create instance of each interceptor -> InjectableInterceptor.get()
            ResultHandle creationalContext = constructor.invokeStaticMethod(MethodDescriptors.CREATIONAL_CTX_CHILD,
                    creationalContextHandle);
            ResultHandle interceptorInstance = constructor.invokeInterfaceMethod(
                    MethodDescriptors.INJECTABLE_REF_PROVIDER_GET, constructorMethodParam, creationalContext);

            // get ID -> InjectableInterceptor.getIdentifier()
            ResultHandle idResultHandle = constructor.invokeInterfaceMethod(MethodDescriptors.GET_IDENTIFIER,
                    constructorMethodParam);
            // then store it in the map -> Map.put(id, instance)
            constructor.invokeInterfaceMethod(MethodDescriptors.MAP_PUT,
                    interceptorInstanceMap, idResultHandle, interceptorInstance);
        }

        // PreDestroy interceptors
        FieldCreator preDestroysField = null;
        InterceptionInfo preDestroys = bean.getLifecycleInterceptors(InterceptionType.PRE_DESTROY);
        if (!preDestroys.isEmpty()) {
            // private final List<InvocationContextImpl.InterceptorInvocation> preDestroys
            preDestroysField = subclass.getFieldCreator("preDestroys", DescriptorUtils.extToInt(ArrayList.class.getName()))
                    .setModifiers(ACC_PRIVATE | ACC_FINAL);
            // preDestroys = new ArrayList<>()
            constructor.writeInstanceField(preDestroysField.getFieldDescriptor(), constructor.getThis(),
                    constructor.newInstance(MethodDescriptor.ofConstructor(ArrayList.class)));
            for (InterceptorInfo interceptor : preDestroys.interceptors) {
                // preDestroys.add(InvocationContextImpl.InterceptorInvocation.preDestroy(provider1,interceptorInstanceMap.get(InjectableInterceptor.getIdentifier())))
                ResultHandle interceptorInstance = constructor.invokeInterfaceMethod(MethodDescriptors.MAP_GET,
                        interceptorInstanceMap, constructor.invokeInterfaceMethod(MethodDescriptors.GET_IDENTIFIER,
                                interceptorToResultHandle.get(interceptor)));
                ResultHandle interceptionInvocation = constructor.invokeStaticMethod(
                        MethodDescriptor.ofMethod(InterceptorInvocation.class, "preDestroy",
                                InterceptorInvocation.class, InjectableInterceptor.class, Object.class),
                        interceptorToResultHandle.get(interceptor),
                        interceptorInstance);
                constructor.invokeInterfaceMethod(MethodDescriptors.LIST_ADD,
                        constructor.readInstanceField(preDestroysField.getFieldDescriptor(), constructor.getThis()),
                        interceptionInvocation);
            }
        }

        // Init intercepted methods and interceptor chains
        // private final Map<String, List<InvocationContextImpl.InterceptorInvocation>> interceptorChains
        FieldCreator interceptorChainsField = subclass.getFieldCreator("interceptorChains", Map.class.getName())
                .setModifiers(ACC_PRIVATE | ACC_FINAL);
        // interceptorChains = new HashMap<>()
        constructor.writeInstanceField(interceptorChainsField.getFieldDescriptor(), constructor.getThis(),
                constructor.newInstance(MethodDescriptor.ofConstructor(HashMap.class)));
        ResultHandle interceptorChainsHandle = constructor.readInstanceField(interceptorChainsField.getFieldDescriptor(),
                constructor.getThis());
        // private final Map<String, Method> methods
        FieldCreator methodsField = subclass.getFieldCreator("methods", DescriptorUtils.extToInt(Map.class.getName()))
                .setModifiers(ACC_PRIVATE | ACC_FINAL);
        constructor.writeInstanceField(methodsField.getFieldDescriptor(), constructor.getThis(),
                constructor.newInstance(MethodDescriptor.ofConstructor(HashMap.class)));
        ResultHandle methodsHandle = constructor.readInstanceField(methodsField.getFieldDescriptor(), constructor.getThis());

        int methodIdx = 1;
        for (Entry<MethodInfo, InterceptionInfo> entry : bean.getInterceptedMethods().entrySet()) {
            String methodId = "m" + methodIdx++;
            MethodInfo method = entry.getKey();
            ResultHandle methodIdHandle = constructor.load(methodId);

            // First create interceptor chains
            // List<InvocationContextImpl.InterceptorInvocation> m1Chain = new ArrayList<>()
            ResultHandle chainHandle = constructor.newInstance(MethodDescriptor.ofConstructor(ArrayList.class));
            InterceptionInfo interceptedMethod = entry.getValue();
            for (InterceptorInfo interceptor : interceptedMethod.interceptors) {
                // m1Chain.add(InvocationContextImpl.InterceptorInvocation.aroundInvoke(p3,interceptorInstanceMap.get(InjectableInterceptor.getIdentifier())))
                ResultHandle interceptorInstance = constructor.invokeInterfaceMethod(MethodDescriptors.MAP_GET,
                        interceptorInstanceMap, constructor.invokeInterfaceMethod(MethodDescriptors.GET_IDENTIFIER,
                                interceptorToResultHandle.get(interceptor)));
                ResultHandle interceptionInvocation = constructor.invokeStaticMethod(
                        MethodDescriptors.INTERCEPTOR_INVOCATION_AROUND_INVOKE,
                        interceptorToResultHandle.get(interceptor), interceptorInstance);
                constructor.invokeInterfaceMethod(MethodDescriptors.LIST_ADD, chainHandle, interceptionInvocation);
            }
            // interceptorChains.put("m1", m1Chain)
            constructor.invokeInterfaceMethod(MethodDescriptors.MAP_PUT, interceptorChainsHandle, methodIdHandle, chainHandle);
            // methods.put("m1", Reflections.findMethod(org.jboss.weld.arc.test.interceptors.SimpleBean.class,"foo",java.lang.String.class))
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
                paramsHandles[2] = constructor.newArray(Class.class, constructor.load(0));
            }
            ResultHandle methodHandle = constructor.invokeStaticMethod(MethodDescriptors.REFLECTIONS_FIND_METHOD,
                    paramsHandles);
            constructor.invokeInterfaceMethod(MethodDescriptors.MAP_PUT, methodsHandle, methodIdHandle, methodHandle);

            // Needed when running on substrate VM
            reflectionRegistration.registerMethod(method);

            // Finally create the forwarding method
            createForwardingMethod(classOutput, bean, method, methodId, subclass, providerTypeName,
                    interceptorChainsField.getFieldDescriptor(),
                    methodsField.getFieldDescriptor(), interceptedMethod);
        }

        constructor.returnValue(null);
        return preDestroysField != null ? preDestroysField.getFieldDescriptor() : null;
    }

    private void createForwardingMethod(ClassOutput classOutput, BeanInfo bean, MethodInfo method, String methodId,
            ClassCreator subclass,
            String providerTypeName, FieldDescriptor interceptorChainsField, FieldDescriptor methodsField,
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
                .ifNull(forwardMethod.readInstanceField(methodsField, forwardMethod.getThis())).trueBranch();
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

        // InvocationContext
        // (java.lang.String) InvocationContextImpl.aroundInvoke(this, methods.get("m1"), params, interceptorChains.get("m1"), forward).proceed()
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
        // InvocationContextImpl.aroundInvoke(this, methods.get("m1"), params, interceptorChains.get("m1"), forward)
        ResultHandle methodIdHandle = tryCatch.load(methodId);
        ResultHandle interceptedMethodHandle = tryCatch.invokeInterfaceMethod(MethodDescriptors.MAP_GET,
                tryCatch.readInstanceField(methodsField, tryCatch.getThis()), methodIdHandle);
        ResultHandle interceptedChainHandle = tryCatch.invokeInterfaceMethod(MethodDescriptors.MAP_GET,
                tryCatch.readInstanceField(interceptorChainsField, tryCatch.getThis()), methodIdHandle);
        // Interceptor bindings
        ResultHandle bindingsHandle = tryCatch.newInstance(MethodDescriptor.ofConstructor(HashSet.class));
        for (AnnotationInstance binding : interceptedMethod.bindings) {
            // Create annotation literals first
            ClassInfo bindingClass = bean.getDeployment().getInterceptorBinding(binding.name());
            tryCatch.invokeInterfaceMethod(MethodDescriptors.SET_ADD, bindingsHandle,
                    annotationLiterals.process(tryCatch, classOutput, bindingClass, binding,
                            Types.getPackageName(subclass.getClassName())));
        }

        ResultHandle invocationContext = tryCatch.invokeStaticMethod(MethodDescriptors.INVOCATION_CONTEXT_AROUND_INVOKE,
                tryCatch.getThis(),
                interceptedMethodHandle, paramsHandle, interceptedChainHandle, func.getInstance(), bindingsHandle);
        // InvocationContext.proceed()
        ResultHandle ret = tryCatch.invokeInterfaceMethod(MethodDescriptors.INVOCATION_CONTEXT_PROCEED, invocationContext);
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
            ResultHandle invocationContext = tryCatch.invokeStaticMethod(MethodDescriptors.INVOCATION_CONTEXT_PRE_DESTROY,
                    tryCatch.getThis(), predestroysHandle,
                    bindingsHandle);

            // InvocationContext.proceed()
            tryCatch.invokeInterfaceMethod(MethodDescriptors.INVOCATION_CONTEXT_PROCEED, invocationContext);
            destroy.returnValue(null);
        }
    }

}
