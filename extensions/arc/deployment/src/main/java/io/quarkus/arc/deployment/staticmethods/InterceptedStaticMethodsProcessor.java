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
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.enterprise.context.spi.Contextual;
import javax.enterprise.inject.spi.InterceptionType;
import javax.interceptor.InvocationContext;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import io.quarkus.arc.InjectableInterceptor;
import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.BeanRegistrationPhaseBuildItem;
import io.quarkus.arc.deployment.InterceptorResolverBuildItem;
import io.quarkus.arc.deployment.TransformedAnnotationsBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.impl.CreationalContextImpl;
import io.quarkus.arc.impl.InterceptedMethodMetadata;
import io.quarkus.arc.impl.InterceptedStaticMethods;
import io.quarkus.arc.impl.InterceptedStaticMethods.InterceptedStaticMethod;
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
import io.quarkus.deployment.util.AsmUtil;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.DescriptorUtils;
import io.quarkus.gizmo.FunctionCreator;
import io.quarkus.gizmo.Gizmo;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;

public class InterceptedStaticMethodsProcessor {

    private static final Logger LOGGER = Logger.getLogger(InterceptedStaticMethodsProcessor.class);

    static final MethodDescriptor INTERCEPTED_STATIC_METHODS_REGISTER = MethodDescriptor
            .ofMethod(InterceptedStaticMethods.class, "register", void.class, String.class, InterceptedStaticMethod.class);
    static final MethodDescriptor INTERCEPTED_STATIC_METHODS_AROUND_INVOKE = MethodDescriptor
            .ofMethod(InterceptedStaticMethods.class, "aroundInvoke", Object.class, String.class, Object[].class);

    private static final String ORGINAL_METHOD_COPY_SUFFIX = "_orig";
    private static final String INITIALIZER_CLASS_SUFFIX = "_InterceptorInitializer";

    @BuildStep
    void collectInterceptedStaticMethods(BeanArchiveIndexBuildItem beanArchiveIndex,
            BuildProducer<InterceptedStaticMethodBuildItem> interceptedStaticMethods,
            InterceptorResolverBuildItem interceptorResolver, TransformedAnnotationsBuildItem transformedAnnotations,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeans) {

        // In this step we collect all intercepted static methods, ie. static methods annotated with interceptor bindings  
        Set<DotName> interceptorBindings = interceptorResolver.getInterceptorBindings();

        for (ClassInfo clazz : beanArchiveIndex.getIndex().getKnownClasses()) {
            for (MethodInfo method : clazz.methods()) {
                // Find all static methods (except for static initializers)
                if (!Modifier.isStatic(method.flags()) || "<clinit>".equals(method.name())) {
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
                    if (annotationInstance.target().kind() == Kind.METHOD
                            && interceptorBindings.contains(annotationInstance.name())) {
                        if (methodLevelBindings == null) {
                            methodLevelBindings = new HashSet<>();
                        }
                        methodLevelBindings.add(annotationInstance);
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
            BuildProducer<GeneratedClassBuildItem> generatedClasses,
            BuildProducer<BytecodeTransformerBuildItem> transformers,
            BuildProducer<ReflectiveMethodBuildItem> reflectiveMethods) {

        if (interceptedStaticMethods.isEmpty()) {
            return;
        }

        ClassOutput classOutput = new GeneratedClassGizmoAdaptor(generatedClasses, true);

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
        Map<DotName, String> initializers = new HashMap<>();
        String initAllMethodName = "init_static_intercepted_methods";
        for (Entry<DotName, List<InterceptedStaticMethodBuildItem>> entry : interceptedStaticMethodsMap.entrySet()) {

            String packageName = DotNames.internalPackageNameWithTrailingSlash(entry.getKey());
            String intializerName = packageName.replace("/", ".") + entry.getKey().withoutPackagePrefix()
                    + INITIALIZER_CLASS_SUFFIX;
            initializers.put(entry.getKey(), intializerName);

            ClassCreator initializer = ClassCreator.builder().classOutput(classOutput)
                    .className(intializerName).setFinal(true).build();

            List<String> initMethods = new ArrayList<>();
            for (InterceptedStaticMethodBuildItem interceptedStaticMethod : entry.getValue()) {
                initMethods.add(implementInit(beanArchiveIndex.getIndex(), classOutput, initializer, interceptedStaticMethod,
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
                    new InterceptedStaticMethodsEnhancer(initializers.get(entry.getKey()), entry.getValue())));
        }

        // Generate a global initializer that calls all other initializers
        ClassCreator globalInitializer = ClassCreator.builder().classOutput(classOutput)
                .className(InterceptedStaticMethodsRecorder.INTIALIZER_CLASS_NAME.replace('.', '/')).setFinal(true).build();

        MethodCreator staticInit = globalInitializer.getMethodCreator("<clinit>", void.class)
                .setModifiers(ACC_STATIC);
        for (String initializerClass : initializers.values()) {
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
        List<Type> params = method.parameters();
        Object[] paramTypes = new String[params.size()];
        for (int i = 0; i < paramTypes.length; ++i) {
            paramTypes[i] = DescriptorUtils.typeToString(params.get(i));
        }
        MethodCreator forward = initializer
                .getMethodCreator(interceptedStaticMethod.getHash(), DescriptorUtils.typeToString(method.returnType()),
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

    private String implementInit(IndexView index, ClassOutput classOutput, ClassCreator initializer,
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
        paramsHandles[0] = init.loadClass(method.declaringClass().name().toString());
        paramsHandles[1] = init.load(method.name());
        if (!method.parameters().isEmpty()) {
            ResultHandle paramsArray = init.newArray(Class.class, init.load(method.parameters().size()));
            for (ListIterator<Type> iterator = method.parameters().listIterator(); iterator.hasNext();) {
                init.writeArrayValue(paramsArray, iterator.nextIndex(),
                        init.loadClass(iterator.next().name().toString()));
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
                    createBindingLiteral(index, classOutput, init, bindings.iterator().next(),
                            beanProcessor.getAnnotationLiteralProcessor()));
        } else {
            bindingsHandle = init.newInstance(MethodDescriptor.ofConstructor(HashSet.class));
            for (AnnotationInstance binding : bindings) {
                init.invokeInterfaceMethod(MethodDescriptors.SET_ADD, bindingsHandle,
                        createBindingLiteral(index, classOutput, init, binding, beanProcessor.getAnnotationLiteralProcessor()));
            }
        }

        // Now create metadata for the given intercepted method
        ResultHandle metadataHandle = init.newInstance(MethodDescriptors.INTERCEPTED_METHOD_METADATA_CONSTRUCTOR,
                chainHandle, methodHandle, bindingsHandle);

        // Needed when running on native image
        reflectiveMethods.produce(new ReflectiveMethodBuildItem(method));

        // Create forwarding function
        ResultHandle forwardingFunc = createForwardingFunction(init, interceptedStaticMethod.getTarget(), method);

        ResultHandle staticMethodHandle = init.newInstance(
                MethodDescriptor.ofConstructor(InterceptedStaticMethod.class, Function.class, InterceptedMethodMetadata.class),
                forwardingFunc, metadataHandle);

        // Call InterceptedStaticMethods.register()
        init.invokeStaticMethod(INTERCEPTED_STATIC_METHODS_REGISTER, init.load(interceptedStaticMethod.getHash()),
                staticMethodHandle);
        init.returnValue(null);
        return name;
    }

    private ResultHandle createBindingLiteral(IndexView index, ClassOutput classOutput, BytecodeCreator init,
            AnnotationInstance binding, AnnotationLiteralProcessor annotationLiteralProcessor) {
        ClassInfo bindingClass = index.getClassByName(binding.name());
        return annotationLiteralProcessor.process(init, classOutput, bindingClass, binding,
                "io.quarkus.arc.runtime");
    }

    private ResultHandle createInterceptorInvocation(InterceptorInfo interceptor, BytecodeCreator init,
            ResultHandle creationalContext) {
        ResultHandle interceptorBean = getInterceptorBean(interceptor, init);
        ResultHandle interceptorInstane = createInterceptor(interceptorBean, init, creationalContext);
        return init.invokeStaticMethod(
                MethodDescriptors.INTERCEPTOR_INVOCATION_AROUND_INVOKE,
                interceptorBean, interceptorInstane);
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
        // Function<InvocationContext, Object> forward = ctx -> Foo.interceptMe_original((java.lang.String)ctx.getParameters()[0])
        FunctionCreator func = init.createFunction(Function.class);
        BytecodeCreator funcBytecode = func.getBytecode();
        List<Type> paramTypes = method.parameters();
        ResultHandle[] paramHandles;
        String[] params;
        if (paramTypes.isEmpty()) {
            paramHandles = new ResultHandle[0];
            params = new String[0];
        } else {
            paramHandles = new ResultHandle[paramTypes.size()];
            ResultHandle ctxHandle = funcBytecode.getMethodParam(0);
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
                MethodDescriptor.ofMethod(target.name().toString(), method.name() + ORGINAL_METHOD_COPY_SUFFIX,
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
            return new InterceptedStaticMethodsClassVisitor(initializerClassName, outputClassVisitor, methods);
        }

    }

    static class InterceptedStaticMethodsClassVisitor extends ClassVisitor {

        private final String initializerClassName;
        private final List<InterceptedStaticMethodBuildItem> methods;

        public InterceptedStaticMethodsClassVisitor(String initializerClassName, ClassVisitor outputClassVisitor,
                List<InterceptedStaticMethodBuildItem> methods) {
            super(Gizmo.ASM_API_VERSION, outputClassVisitor);
            this.methods = methods;
            this.initializerClassName = initializerClassName;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            InterceptedStaticMethodBuildItem method = findMatchingMethod(access, name, descriptor);
            if (method != null) {
                MethodVisitor copy = super.visitMethod(access,
                        name + ORGINAL_METHOD_COPY_SUFFIX,
                        descriptor,
                        signature,
                        exceptions);
                MethodVisitor superVisitor = super.visitMethod(access, name, descriptor, signature, exceptions);
                return new InterceptedStaticMethodsMethodVisitor(superVisitor, copy, initializerClassName, method);
            } else {
                return super.visitMethod(access, name, descriptor, signature, exceptions);
            }
        }

        private InterceptedStaticMethodBuildItem findMatchingMethod(int access, String name, String descriptor) {
            if (Modifier.isStatic(access)) {
                for (InterceptedStaticMethodBuildItem method : methods) {
                    if (method.getMethod().name().equals(name)
                            && MethodDescriptor.of(method.getMethod()).getDescriptor().equals(descriptor)) {
                        return method;
                    }
                }
            }
            return null;
        }

    }

    static class InterceptedStaticMethodsMethodVisitor extends MethodVisitor {

        private final String initializerClassName;
        private final InterceptedStaticMethodBuildItem interceptedStaticMethod;
        private final MethodVisitor superVisitor;

        public InterceptedStaticMethodsMethodVisitor(MethodVisitor superVisitor, MethodVisitor copyVisitor,
                String initializerClassName, InterceptedStaticMethodBuildItem interceptedStaticMethod) {
            super(Gizmo.ASM_API_VERSION, copyVisitor);
            this.superVisitor = superVisitor;
            this.initializerClassName = initializerClassName;
            this.interceptedStaticMethod = interceptedStaticMethod;
        }

        @Override
        public void visitEnd() {
            // Invoke the initializer, i.e. Foo_InterceptorInitializer.hash("ping")
            MethodDescriptor descriptor = MethodDescriptor.of(interceptedStaticMethod.getMethod());
            int paramSlot = 0;
            for (Type paramType : interceptedStaticMethod.getMethod().parameters()) {
                superVisitor.visitIntInsn(AsmUtil.getLoadOpcode(paramType), paramSlot);
                paramSlot += AsmUtil.getParameterSize(paramType);
            }
            superVisitor.visitMethodInsn(Opcodes.INVOKESTATIC,
                    initializerClassName.replace('.', '/'), interceptedStaticMethod.getHash(),
                    descriptor.getDescriptor().toString(),
                    false);
            superVisitor.visitInsn(AsmUtil.getReturnInstruction(interceptedStaticMethod.getMethod().returnType()));
            superVisitor.visitMaxs(0, 0);
            superVisitor.visitEnd();

            super.visitEnd();
        }

    }

}
