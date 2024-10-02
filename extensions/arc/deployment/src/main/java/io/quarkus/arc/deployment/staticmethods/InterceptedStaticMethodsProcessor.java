package io.quarkus.arc.deployment.staticmethods;

import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.enterprise.context.spi.Contextual;
import jakarta.enterprise.inject.spi.InterceptionType;
import jakarta.interceptor.InvocationContext;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.MethodParameterInfo;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;
import org.objectweb.asm.ClassVisitor;

import io.quarkus.arc.InjectableInterceptor;
import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.BeanRegistrationPhaseBuildItem;
import io.quarkus.arc.deployment.CompletedApplicationClassPredicateBuildItem;
import io.quarkus.arc.deployment.InterceptorResolverBuildItem;
import io.quarkus.arc.deployment.TransformedAnnotationsBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.impl.CreationalContextImpl;
import io.quarkus.arc.impl.InterceptedMethodMetadata;
import io.quarkus.arc.impl.InterceptedStaticMethods;
import io.quarkus.arc.processor.AnnotationLiteralProcessor;
import io.quarkus.arc.processor.BeanProcessor;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.arc.processor.InterceptorInfo;
import io.quarkus.arc.processor.MethodDescriptors;
import io.quarkus.arc.runtime.InterceptedStaticMethodsRecorder;
import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Produce;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveMethodBuildItem;
import io.quarkus.gizmo.AnnotatedElement;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.ClassTransformer;
import io.quarkus.gizmo.DescriptorUtils;
import io.quarkus.gizmo.FunctionCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;

public class InterceptedStaticMethodsProcessor {

    private static final Logger LOGGER = Logger.getLogger(InterceptedStaticMethodsProcessor.class);

    static final MethodDescriptor INTERCEPTED_STATIC_METHODS_REGISTER = MethodDescriptor
            .ofMethod(InterceptedStaticMethods.class, "register", void.class, String.class, InterceptedMethodMetadata.class);
    static final MethodDescriptor INTERCEPTED_STATIC_METHODS_AROUND_INVOKE = MethodDescriptor
            .ofMethod(InterceptedStaticMethods.class, "aroundInvoke", Object.class, String.class, Object[].class);

    private static final String ORIGINAL_METHOD_COPY_SUFFIX = "_orig";
    private static final String INITIALIZER_CLASS_SUFFIX = "_InterceptorInitializer";

    @BuildStep
    void collectInterceptedStaticMethods(BeanArchiveIndexBuildItem beanArchiveIndex,
            BuildProducer<InterceptedStaticMethodBuildItem> interceptedStaticMethods,
            InterceptorResolverBuildItem interceptorResolver, TransformedAnnotationsBuildItem transformedAnnotations,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeans) {
        // In this step we collect all intercepted static methods, i.e. static methods annotated with interceptor bindings

        for (ClassInfo clazz : beanArchiveIndex.getIndex().getKnownClasses()) {
            for (MethodInfo method : clazz.methods()) {
                // Find all non-synthetic static methods (except for static initializers)
                if (method.isSynthetic()
                        || !Modifier.isStatic(method.flags())
                        || "<clinit>".equals(method.name())) {
                    continue;
                }
                // Get the (possibly transformed) set of annotations
                Collection<AnnotationInstance> annotations = transformedAnnotations.getAnnotations(method);
                if (annotations.isEmpty()) {
                    continue;
                }
                // Only method-level bindings are considered due to backwards compatibility
                Set<AnnotationInstance> methodLevelBindings = null;
                for (AnnotationInstance annotationInstance : annotations) {
                    if (annotationInstance.target().kind() == Kind.METHOD) {
                        Collection<AnnotationInstance> bindings = interceptorResolver
                                .extractInterceptorBindings(annotationInstance);
                        if (!bindings.isEmpty()) {
                            if (methodLevelBindings == null) {
                                methodLevelBindings = new HashSet<>();
                            }
                            methodLevelBindings.addAll(bindings);
                        }
                    }
                }
                if (methodLevelBindings == null || methodLevelBindings.isEmpty()) {
                    continue;
                }
                if (Modifier.isPrivate(method.flags())) {
                    LOGGER.warnf(
                            "Interception of private static methods is not supported; bindings found on %s: %s",
                            method.declaringClass().name(),
                            method);
                } else {
                    List<InterceptorInfo> interceptors = interceptorResolver.get().resolve(
                            InterceptionType.AROUND_INVOKE,
                            methodLevelBindings);
                    if (!interceptors.isEmpty()) {
                        LOGGER.debugf("Intercepted static method found on %s: %s", method.declaringClass().name(),
                                method);
                        unremovableBeans.produce(UnremovableBeanBuildItem.beanClassNames(interceptors.stream()
                                .map(InterceptorInfo::getBeanClass).map(Object::toString).collect(Collectors.toSet())));
                        interceptedStaticMethods.produce(
                                new InterceptedStaticMethodBuildItem(method, methodLevelBindings, interceptors));
                    }
                }
            }
        }
    }

    @Produce(InterceptedStaticMethodsTransformersRegisteredBuildItem.class)
    @BuildStep
    void processInterceptedStaticMethods(BeanArchiveIndexBuildItem beanArchiveIndex,
            BeanRegistrationPhaseBuildItem phase,
            List<InterceptedStaticMethodBuildItem> interceptedStaticMethods,
            CompletedApplicationClassPredicateBuildItem applicationClassPredicate,
            BuildProducer<GeneratedClassBuildItem> generatedClasses,
            BuildProducer<BytecodeTransformerBuildItem> transformers,
            BuildProducer<ReflectiveMethodBuildItem> reflectiveMethods) {

        if (interceptedStaticMethods.isEmpty()) {
            return;
        }

        // org.acme.Foo -> org.acme.Foo_InterceptorInitializer
        Map<DotName, String> baseToGeneratedInitializer = new HashMap<>();
        ClassOutput classOutput = new GeneratedClassGizmoAdaptor(generatedClasses, new Predicate<String>() {

            @Override
            public boolean test(String name) {
                // For example, for base org.acme.Foo we generate org.acme.Foo_InterceptorInitializer
                // and possibly anonymous classes like org.acme.Foo_InterceptorInitializer$$function$$1
                name = name.replace('/', '.');
                DotName base = null;
                for (Entry<DotName, String> e : baseToGeneratedInitializer.entrySet()) {
                    if (e.getValue().equals(name) || name.startsWith(e.getValue())) {
                        base = e.getKey();
                    }
                }
                if (base == null) {
                    throw new IllegalStateException("Unable to find the base class for the generated: " + name);
                }
                return applicationClassPredicate.test(base);
            }
        });

        // declaring class -> intercepted static methods
        Map<DotName, List<InterceptedStaticMethodBuildItem>> interceptedStaticMethodsMap = new HashMap<>();
        for (InterceptedStaticMethodBuildItem interceptedStaticMethod : interceptedStaticMethods) {
            List<InterceptedStaticMethodBuildItem> list = interceptedStaticMethodsMap
                    .get(interceptedStaticMethod.getTarget().name());
            if (list == null) {
                list = new ArrayList<>();
                interceptedStaticMethodsMap.put(interceptedStaticMethod.getTarget().name(), list);
            }
            list.add(interceptedStaticMethod);
        }

        // For each declaring class create an initializer class that:
        // 1. registers all interceptor chains inside an "init_static_intercepted_methods" method
        // 2. adds static methods to invoke the interceptor chain and delegate to the copy of the original static method
        // declaring class -> initializer class

        String initAllMethodName = "init_static_intercepted_methods";
        for (Entry<DotName, List<InterceptedStaticMethodBuildItem>> entry : interceptedStaticMethodsMap.entrySet()) {

            String packageName = DotNames.internalPackageNameWithTrailingSlash(entry.getKey());
            String initializerName = packageName.replace("/", ".") + entry.getKey().withoutPackagePrefix()
                    + INITIALIZER_CLASS_SUFFIX;
            baseToGeneratedInitializer.put(entry.getKey(), initializerName);

            ClassCreator initializer = ClassCreator.builder().classOutput(classOutput)
                    .className(initializerName).setFinal(true).build();

            List<String> initMethods = new ArrayList<>();
            for (InterceptedStaticMethodBuildItem interceptedStaticMethod : entry.getValue()) {
                initMethods.add(implementInit(beanArchiveIndex.getIndex(), initializer, interceptedStaticMethod,
                        reflectiveMethods, phase.getBeanProcessor()));
                implementForward(initializer, interceptedStaticMethod);
            }

            MethodCreator init = initializer.getMethodCreator(initAllMethodName, void.class)
                    .setModifiers(ACC_PUBLIC | ACC_STATIC);
            for (String initMethod : initMethods) {
                init.invokeStaticMethod(
                        MethodDescriptor.ofMethod(initializer.getClassName(), initMethod, void.class));

            }
            init.returnValue(null);
            initializer.close();
        }

        // Transform all declaring classes
        // For each intercepted static methods create a copy and modify the original method to delegate to the relevant initializer
        for (Entry<DotName, List<InterceptedStaticMethodBuildItem>> entry : interceptedStaticMethodsMap.entrySet()) {
            transformers.produce(new BytecodeTransformerBuildItem(entry.getKey().toString(),
                    new InterceptedStaticMethodsEnhancer(baseToGeneratedInitializer.get(entry.getKey()), entry.getValue())));
        }

        // Generate a global initializer that calls all other initializers; this initializer must be loaded by the runtime ClassLoader
        ClassCreator globalInitializer = ClassCreator.builder()
                .classOutput(new GeneratedClassGizmoAdaptor(generatedClasses, true))
                .className(InterceptedStaticMethodsRecorder.INITIALIZER_CLASS_NAME.replace('.', '/')).setFinal(true).build();

        MethodCreator staticInit = globalInitializer.getMethodCreator("<clinit>", void.class)
                .setModifiers(ACC_STATIC);
        for (String initializerClass : baseToGeneratedInitializer.values()) {
            staticInit.invokeStaticMethod(
                    MethodDescriptor.ofMethod(initializerClass, initAllMethodName, void.class));
        }
        staticInit.returnValue(null);
        globalInitializer.close();
    }

    @Record(STATIC_INIT)
    @BuildStep
    void callInitializer(BeanContainerBuildItem beanContainer, List<InterceptedStaticMethodBuildItem> interceptedStaticMethods,
            InterceptedStaticMethodsRecorder recorder) {
        if (interceptedStaticMethods.isEmpty()) {
            return;
        }
        recorder.callInitializer();
    }

    private void implementForward(ClassCreator initializer,
            InterceptedStaticMethodBuildItem interceptedStaticMethod) {
        MethodInfo method = interceptedStaticMethod.getMethod();
        List<Type> params = method.parameterTypes();
        Object[] paramTypes = new String[params.size()];
        for (int i = 0; i < paramTypes.length; ++i) {
            paramTypes[i] = DescriptorUtils.typeToString(params.get(i));
        }
        MethodCreator forward = initializer
                .getMethodCreator(interceptedStaticMethod.getForwardingMethodName(),
                        DescriptorUtils.typeToString(method.returnType()),
                        paramTypes)
                .setModifiers(ACC_PUBLIC | ACC_FINAL | ACC_STATIC);
        ResultHandle argArray = forward.newArray(Object.class, params.size());
        for (int i = 0; i < params.size(); i++) {
            forward.writeArrayValue(argArray, i, forward.getMethodParam(i));
        }
        ResultHandle ret = forward.invokeStaticMethod(INTERCEPTED_STATIC_METHODS_AROUND_INVOKE,
                forward.load(interceptedStaticMethod.getHash()), argArray);
        forward.returnValue(ret);
    }

    private String implementInit(IndexView index, ClassCreator initializer,
            InterceptedStaticMethodBuildItem interceptedStaticMethod,
            BuildProducer<ReflectiveMethodBuildItem> reflectiveMethods, BeanProcessor beanProcessor) {

        MethodInfo method = interceptedStaticMethod.getMethod();
        List<InterceptorInfo> interceptors = interceptedStaticMethod.getInterceptors();
        Set<AnnotationInstance> bindings = interceptedStaticMethod.getBindings();

        // init_interceptMe_hash()
        String name = new StringBuilder("init")
                .append("_")
                .append(method.name())
                .append("_")
                .append(interceptedStaticMethod.getHash()).toString();

        MethodCreator init = initializer.getMethodCreator(name, void.class)
                .setModifiers(ACC_PRIVATE | ACC_FINAL | ACC_STATIC);
        ResultHandle creationalContext = init.newInstance(
                MethodDescriptor.ofConstructor(CreationalContextImpl.class, Contextual.class), init.loadNull());

        // 1. Interceptor chain
        ResultHandle chainHandle;
        if (interceptors.size() == 1) {
            // List<InvocationContextImpl.InterceptorInvocation> m1Chain = Collections.singletonList(...);
            chainHandle = init.invokeStaticMethod(MethodDescriptors.COLLECTIONS_SINGLETON_LIST,
                    createInterceptorInvocation(interceptors.get(0), init, creationalContext));
        } else {
            // List<InvocationContextImpl.InterceptorInvocation> m1Chain = new ArrayList<>();
            chainHandle = init.newInstance(MethodDescriptor.ofConstructor(ArrayList.class));
            for (InterceptorInfo interceptor : interceptors) {
                // m1Chain.add(InvocationContextImpl.InterceptorInvocation.aroundInvoke(p3,interceptorInstanceMap.get(InjectableInterceptor.getIdentifier())))
                init.invokeInterfaceMethod(MethodDescriptors.LIST_ADD, chainHandle,
                        createInterceptorInvocation(interceptor, init, creationalContext));
            }
        }

        // 2. Method method = Reflections.findMethod(...)
        ResultHandle[] paramsHandles = new ResultHandle[3];
        paramsHandles[0] = init.loadClassFromTCCL(method.declaringClass().name().toString());
        paramsHandles[1] = init.load(method.name());
        if (!method.parameterTypes().isEmpty()) {
            ResultHandle paramsArray = init.newArray(Class.class, init.load(method.parametersCount()));
            for (ListIterator<Type> iterator = method.parameterTypes().listIterator(); iterator.hasNext();) {
                init.writeArrayValue(paramsArray, iterator.nextIndex(),
                        init.loadClassFromTCCL(iterator.next().name().toString()));
            }
            paramsHandles[2] = paramsArray;
        } else {
            paramsHandles[2] = init.newArray(Class.class, init.load(0));
        }
        ResultHandle methodHandle = init.invokeStaticMethod(MethodDescriptors.REFLECTIONS_FIND_METHOD,
                paramsHandles);

        // 3. Interceptor bindings
        ResultHandle bindingsHandle;
        if (bindings.size() == 1) {
            bindingsHandle = init.invokeStaticMethod(MethodDescriptors.COLLECTIONS_SINGLETON,
                    createBindingLiteral(index, init, bindings.iterator().next(),
                            beanProcessor.getAnnotationLiteralProcessor()));
        } else {
            bindingsHandle = init.newInstance(MethodDescriptor.ofConstructor(HashSet.class));
            for (AnnotationInstance binding : bindings) {
                init.invokeInterfaceMethod(MethodDescriptors.SET_ADD, bindingsHandle,
                        createBindingLiteral(index, init, binding, beanProcessor.getAnnotationLiteralProcessor()));
            }
        }

        // Create forwarding function
        ResultHandle forwardingFunc = createForwardingFunction(init, interceptedStaticMethod.getTarget(), method);

        // Now create metadata for the given intercepted method
        ResultHandle metadataHandle = init.newInstance(MethodDescriptors.INTERCEPTED_METHOD_METADATA_CONSTRUCTOR,
                chainHandle, methodHandle, bindingsHandle, forwardingFunc);

        // Needed when running on native image
        reflectiveMethods.produce(new ReflectiveMethodBuildItem(getClass().getName(), method));

        // Call InterceptedStaticMethods.register()
        init.invokeStaticMethod(INTERCEPTED_STATIC_METHODS_REGISTER, init.load(interceptedStaticMethod.getHash()),
                metadataHandle);
        init.returnValue(null);
        return name;
    }

    private ResultHandle createBindingLiteral(IndexView index, BytecodeCreator init,
            AnnotationInstance binding, AnnotationLiteralProcessor annotationLiterals) {
        ClassInfo bindingClass = index.getClassByName(binding.name());
        return annotationLiterals.create(init, bindingClass, binding);
    }

    private ResultHandle createInterceptorInvocation(InterceptorInfo interceptor, BytecodeCreator init,
            ResultHandle creationalContext) {
        ResultHandle interceptorBean = getInterceptorBean(interceptor, init);
        ResultHandle interceptorInstance = createInterceptor(interceptorBean, init, creationalContext);
        return init.invokeStaticMethod(
                MethodDescriptors.INTERCEPTOR_INVOCATION_AROUND_INVOKE,
                interceptorBean, interceptorInstance);
    }

    private ResultHandle getInterceptorBean(InterceptorInfo interceptor, BytecodeCreator creator) {
        ResultHandle containerHandle = creator
                .invokeStaticMethod(MethodDescriptors.ARC_CONTAINER);
        return creator.checkCast(creator.invokeInterfaceMethod(
                MethodDescriptors.ARC_CONTAINER_BEAN,
                containerHandle, creator.load(interceptor.getIdentifier())), InjectableInterceptor.class);
    }

    private ResultHandle createInterceptor(ResultHandle interceptorBean, BytecodeCreator creator,
            ResultHandle parentCreationalContext) {
        ResultHandle creationalContext = creator.invokeStaticMethod(MethodDescriptors.CREATIONAL_CTX_CHILD,
                parentCreationalContext);
        return creator.invokeInterfaceMethod(
                MethodDescriptors.INJECTABLE_REF_PROVIDER_GET, interceptorBean, creationalContext);
    }

    private ResultHandle createForwardingFunction(MethodCreator init, ClassInfo target, MethodInfo method) {
        // Forwarding function
        // BiFunction<Object, InvocationContext, Object> forward = (ignored, ctx) -> Foo.interceptMe_original((java.lang.String)ctx.getParameters()[0])
        FunctionCreator func = init.createFunction(BiFunction.class);
        BytecodeCreator funcBytecode = func.getBytecode();
        List<Type> paramTypes = method.parameterTypes();
        ResultHandle[] paramHandles;
        String[] params;
        if (paramTypes.isEmpty()) {
            paramHandles = new ResultHandle[0];
            params = new String[0];
        } else {
            paramHandles = new ResultHandle[paramTypes.size()];
            ResultHandle ctxHandle = funcBytecode.getMethodParam(1);
            ResultHandle ctxParamsHandle = funcBytecode.invokeInterfaceMethod(
                    MethodDescriptor.ofMethod(InvocationContext.class, "getParameters", Object[].class),
                    ctxHandle);
            // autoboxing is handled inside Gizmo
            for (int i = 0; i < paramHandles.length; i++) {
                paramHandles[i] = funcBytecode.readArrayValue(ctxParamsHandle, i);
            }
            params = new String[paramTypes.size()];
            for (int i = 0; i < paramTypes.size(); i++) {
                params[i] = paramTypes.get(i).name().toString();
            }
        }
        ResultHandle ret = funcBytecode.invokeStaticMethod(
                MethodDescriptor.ofMethod(target.name().toString(), method.name() + ORIGINAL_METHOD_COPY_SUFFIX,
                        method.returnType().name().toString(),
                        params),
                paramHandles);
        if (ret == null) {
            funcBytecode.returnValue(funcBytecode.loadNull());
        } else {
            funcBytecode.returnValue(ret);
        }
        return func.getInstance();
    }

    static class InterceptedStaticMethodsEnhancer implements BiFunction<String, ClassVisitor, ClassVisitor> {

        private final String initializerClassName;
        private final List<InterceptedStaticMethodBuildItem> methods;

        public InterceptedStaticMethodsEnhancer(String initializerClassName, List<InterceptedStaticMethodBuildItem> methods) {
            this.methods = methods;
            this.initializerClassName = initializerClassName;
        }

        @Override
        public ClassVisitor apply(String className, ClassVisitor outputClassVisitor) {
            ClassTransformer transformer = new ClassTransformer(className);
            for (InterceptedStaticMethodBuildItem interceptedStaticMethod : methods) {
                MethodInfo interceptedMethod = interceptedStaticMethod.getMethod();
                MethodDescriptor originalDescriptor = MethodDescriptor.of(interceptedMethod);
                // Rename the intercepted method
                transformer.modifyMethod(originalDescriptor)
                        .rename(interceptedMethod.name() + ORIGINAL_METHOD_COPY_SUFFIX);

                // Add the intercepted method again - invoke the initializer in the body, e.g. Foo_InterceptorInitializer.hash("ping")
                MethodCreator newMethod = transformer.addMethod(originalDescriptor)
                        .setModifiers(interceptedMethod.flags())
                        .setSignature(interceptedMethod.genericSignatureIfRequired());
                // Copy over all annotations with RetentionPolicy.RUNTIME
                for (AnnotationInstance annotationInstance : interceptedMethod.declaredAnnotations()) {
                    if (annotationInstance.runtimeVisible()) {
                        newMethod.addAnnotation(annotationInstance);
                    }
                }
                for (MethodParameterInfo param : interceptedMethod.parameters()) {
                    AnnotatedElement newParam = newMethod.getParameterAnnotations(param.position());
                    for (AnnotationInstance paramAnnotation : param.declaredAnnotations()) {
                        if (paramAnnotation.runtimeVisible()) {
                            newParam.addAnnotation(paramAnnotation);
                        }
                    }
                }
                for (Type exceptionType : interceptedMethod.exceptions()) {
                    newMethod.addException(exceptionType.name().toString());
                }
                ResultHandle[] args = new ResultHandle[interceptedMethod.parametersCount()];
                for (int i = 0; i < interceptedMethod.parametersCount(); ++i) {
                    args[i] = newMethod.getMethodParam(i);
                }
                ResultHandle ret = newMethod.invokeStaticMethod(MethodDescriptor.ofMethod(initializerClassName,
                        interceptedStaticMethod.getForwardingMethodName(),
                        interceptedMethod.returnType().descriptor(),
                        interceptedMethod.parameterTypes().stream().map(Type::descriptor).toArray()),
                        args);
                newMethod.returnValue(ret);
            }
            return transformer.applyTo(outputClassVisitor);
        }

    }

}
