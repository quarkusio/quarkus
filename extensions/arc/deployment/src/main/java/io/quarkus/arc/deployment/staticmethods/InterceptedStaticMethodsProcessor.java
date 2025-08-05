package io.quarkus.arc.deployment.staticmethods;

import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;
import static org.jboss.jandex.gizmo2.Jandex2Gizmo.classDescOf;

import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.enterprise.context.spi.Contextual;
import jakarta.enterprise.context.spi.CreationalContext;
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

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.InjectableInterceptor;
import io.quarkus.arc.InjectableReferenceProvider;
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
import io.quarkus.arc.impl.InterceptorInvocation;
import io.quarkus.arc.impl.Reflections;
import io.quarkus.arc.processor.AnnotationLiteralProcessor;
import io.quarkus.arc.processor.BeanProcessor;
import io.quarkus.arc.processor.InterceptorInfo;
import io.quarkus.arc.runtime.InterceptedStaticMethodsRecorder;
import io.quarkus.deployment.GeneratedClassGizmo2Adaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Produce;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveMethodBuildItem;
import io.quarkus.gizmo.AnnotatedElement;
import io.quarkus.gizmo.ClassTransformer;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.gizmo2.ClassOutput;
import io.quarkus.gizmo2.Const;
import io.quarkus.gizmo2.Expr;
import io.quarkus.gizmo2.Gizmo;
import io.quarkus.gizmo2.LocalVar;
import io.quarkus.gizmo2.ParamVar;
import io.quarkus.gizmo2.Reflection2Gizmo;
import io.quarkus.gizmo2.creator.BlockCreator;
import io.quarkus.gizmo2.desc.ClassMethodDesc;
import io.quarkus.gizmo2.desc.ConstructorDesc;
import io.quarkus.gizmo2.desc.InterfaceMethodDesc;
import io.quarkus.gizmo2.desc.MethodDesc;

public class InterceptedStaticMethodsProcessor {

    private static final Logger LOGGER = Logger.getLogger(InterceptedStaticMethodsProcessor.class);

    static final MethodDesc INTERCEPTED_STATIC_METHODS_REGISTER = MethodDesc.of(InterceptedStaticMethods.class,
            "register", void.class, String.class, InterceptedMethodMetadata.class);

    static final MethodDesc INTERCEPTED_STATIC_METHODS_AROUND_INVOKE = MethodDesc.of(InterceptedStaticMethods.class,
            "aroundInvoke", Object.class, String.class, Object[].class);

    static final MethodDesc ARC_REQUIRE_CONTAINER = MethodDesc.of(Arc.class, "requireContainer", ArcContainer.class);

    static final MethodDesc ARC_CONTAINER_BEAN = MethodDesc.of(ArcContainer.class,
            "bean", InjectableBean.class, String.class);

    static final MethodDesc CREATIONAL_CTX_CHILD = MethodDesc.of(CreationalContextImpl.class,
            "child", CreationalContextImpl.class, CreationalContext.class);

    static final MethodDesc INJECTABLE_REF_PROVIDER_GET = MethodDesc.of(InjectableReferenceProvider.class,
            "get", Object.class, CreationalContext.class);

    static final ConstructorDesc INTERCEPTED_METHOD_METADATA_CONSTRUCTOR = ConstructorDesc.of(InterceptedMethodMetadata.class,
            List.class, Method.class, Set.class, BiFunction.class);

    static final MethodDesc INTERCEPTOR_INVOCATION_AROUND_INVOKE = MethodDesc.of(InterceptorInvocation.class,
            "aroundInvoke", InterceptorInvocation.class, InjectableInterceptor.class, Object.class);

    static final MethodDesc REFLECTIONS_FIND_METHOD = MethodDesc.of(Reflections.class,
            "findMethod", Method.class, Class.class, String.class, Class[].class);

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
                        || method.isStaticInitializer()) {
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
            BuildProducer<GeneratedResourceBuildItem> generatedResources,
            BuildProducer<BytecodeTransformerBuildItem> transformers,
            BuildProducer<ReflectiveMethodBuildItem> reflectiveMethods) {

        if (interceptedStaticMethods.isEmpty()) {
            return;
        }

        // org.acme.Foo -> org.acme.Foo_InterceptorInitializer
        Map<DotName, String> baseToGeneratedInitializer = new HashMap<>();

        ClassOutput classOutput = new GeneratedClassGizmo2Adaptor(generatedClasses, generatedResources,
                new Predicate<String>() {
                    @Override
                    public boolean test(String name) {
                        if (InterceptedStaticMethodsRecorder.INITIALIZER_CLASS_NAME.equals(name)) {
                            return true;
                        }

                        // for base org.acme.Foo we generate org.acme.Foo_InterceptorInitializer
                        // and possibly anonymous classes like org.acme.Foo_InterceptorInitializer$$function$$1
                        DotName base = null;
                        for (Map.Entry<DotName, String> e : baseToGeneratedInitializer.entrySet()) {
                            if (name.equals(e.getValue()) || name.startsWith(e.getValue())) {
                                base = e.getKey();
                            }
                        }
                        if (base == null) {
                            throw new IllegalStateException("Unable to find the base class for " + name);
                        }
                        return applicationClassPredicate.test(base);
                    }
                });
        Gizmo gizmo = Gizmo.create(classOutput);

        // declaring class -> intercepted static methods
        Map<DotName, List<InterceptedStaticMethodBuildItem>> interceptedStaticMethodsMap = new HashMap<>();
        for (InterceptedStaticMethodBuildItem interceptedStaticMethod : interceptedStaticMethods) {
            List<InterceptedStaticMethodBuildItem> list = interceptedStaticMethodsMap.computeIfAbsent(
                    interceptedStaticMethod.getTarget().name(), k -> new ArrayList<>());
            list.add(interceptedStaticMethod);
        }

        // For each declaring class create an initializer class that:
        // 1. registers all interceptor chains inside an "init_static_intercepted_methods" method
        // 2. adds static methods to invoke the interceptor chain and delegate to the copy of the original static method
        // declaring class -> initializer class

        String initAllMethodName = "init_static_intercepted_methods";
        for (Map.Entry<DotName, List<InterceptedStaticMethodBuildItem>> entry : interceptedStaticMethodsMap.entrySet()) {
            String initializerName = entry.getKey() + INITIALIZER_CLASS_SUFFIX;
            baseToGeneratedInitializer.put(entry.getKey(), initializerName);

            gizmo.class_(initializerName, cc -> {
                cc.final_();

                List<MethodDesc> initMethods = new ArrayList<>();
                for (InterceptedStaticMethodBuildItem interceptedStaticMethod : entry.getValue()) {
                    initMethods.add(generateInit(beanArchiveIndex.getIndex(), cc, interceptedStaticMethod,
                            reflectiveMethods, phase.getBeanProcessor()));
                    generateForward(cc, interceptedStaticMethod);
                }

                cc.staticMethod(initAllMethodName, mc -> {
                    mc.body(bc -> {
                        for (MethodDesc initMethod : initMethods) {
                            bc.invokeStatic(initMethod);
                        }
                        bc.return_();
                    });
                });
            });
        }

        // Transform all declaring classes
        // For each intercepted static methods create a copy and modify the original method to delegate to the relevant initializer
        for (Map.Entry<DotName, List<InterceptedStaticMethodBuildItem>> entry : interceptedStaticMethodsMap.entrySet()) {
            transformers.produce(new BytecodeTransformerBuildItem(entry.getKey().toString(),
                    new InterceptedStaticMethodsEnhancer(baseToGeneratedInitializer.get(entry.getKey()), entry.getValue())));
        }

        // Generate a global initializer that calls all other initializers; this initializer must be loaded by the runtime ClassLoader
        gizmo.class_(InterceptedStaticMethodsRecorder.INITIALIZER_CLASS_NAME, cc -> {
            cc.final_();

            cc.staticInitializer(bc -> {
                for (String initializerClass : baseToGeneratedInitializer.values()) {
                    bc.invokeStatic(ClassMethodDesc.of(ClassDesc.of(initializerClass), initAllMethodName, void.class));
                }
                bc.return_();
            });
        });
    }

    private MethodDesc generateInit(IndexView index, io.quarkus.gizmo2.creator.ClassCreator cc,
            InterceptedStaticMethodBuildItem interceptedStaticMethod,
            BuildProducer<ReflectiveMethodBuildItem> reflectiveMethods, BeanProcessor beanProcessor) {

        MethodInfo btMethod = interceptedStaticMethod.getMethod();
        List<InterceptorInfo> interceptors = interceptedStaticMethod.getInterceptors();
        Set<AnnotationInstance> btBindings = interceptedStaticMethod.getBindings();

        // init_interceptMe_hash()
        return cc.staticMethod("init_" + btMethod.name() + "_" + interceptedStaticMethod.getHash(), mc -> {
            mc.private_();

            mc.body(b0 -> {
                LocalVar creationalContext = b0.localVar("creationalContext",
                        b0.new_(ConstructorDesc.of(CreationalContextImpl.class, Contextual.class),
                                Const.ofNull(Contextual.class)));

                // 1. Interceptor chain
                LocalVar chain = b0.localVar("chain", b0.blockExpr(List.class, b1 -> {
                    if (interceptors.size() == 1) {
                        // List<InvocationContextImpl.InterceptorInvocation> chain = List.of(...);
                        b1.yield(b1.listOf(createInterceptorInvocation(interceptors.get(0), b1, creationalContext)));
                    } else {
                        // List<InvocationContextImpl.InterceptorInvocation> chain = new ArrayList<>();
                        LocalVar list = b1.localVar("list", b1.new_(ArrayList.class));
                        for (InterceptorInfo interceptor : interceptors) {
                            // m1Chain.add(InvocationContextImpl.InterceptorInvocation.aroundInvoke(p3,interceptorInstanceMap.get(InjectableInterceptor.getIdentifier())))
                            b1.withList(list).add(createInterceptorInvocation(interceptor, b1, creationalContext));
                        }
                        b1.yield(list);
                    }
                }));

                // 2. Method method = Reflections.findMethod(...)
                LocalVar rtMethod = b0.localVar("method", b0.blockExpr(Reflection2Gizmo.classDescOf(Method.class), b1 -> {
                    // TODO load from TCCL
                    Expr className = Const.of(classDescOf(btMethod.declaringClass()));
                    Expr name = Const.of(btMethod.name());
                    // TODO load from TCCL
                    Expr params = b1.newArray(Class.class, btMethod.parameterTypes(), type -> Const.of(classDescOf(type)));
                    b1.yield(b1.invokeStatic(REFLECTIONS_FIND_METHOD, className, name, params));
                }));

                // 3. Interceptor bindings
                LocalVar rtBindings = b0.localVar("bindings", b0.blockExpr(Set.class, b1 -> {
                    if (btBindings.size() == 1) {
                        b1.yield(b1.setOf(createBindingLiteral(index, b1, btBindings.iterator().next(),
                                beanProcessor.getAnnotationLiteralProcessor())));
                    } else {
                        LocalVar set = b1.localVar("set", b1.new_(HashSet.class));
                        for (AnnotationInstance btBinding : btBindings) {
                            b1.withSet(set).add(createBindingLiteral(index, b1, btBinding,
                                    beanProcessor.getAnnotationLiteralProcessor()));
                        }
                        b1.yield(set);
                    }
                }));

                // 4. Forwarding function
                // BiFunction<Object, InvocationContext, Object> forward = (ignored, ctx) -> Foo.interceptMe_original((java.lang.String)ctx.getParameters()[0])
                Expr forwardingFunc = b0.lambda(BiFunction.class, lc -> {
                    lc.parameter("ignored", 0);
                    ParamVar ctx = lc.parameter("ctx", 1);
                    lc.body(lbc -> {
                        ClassDesc[] params;
                        Expr[] args;
                        if (btMethod.parametersCount() == 0) {
                            params = new ClassDesc[0];
                            args = new Expr[0];
                        } else {
                            params = new ClassDesc[btMethod.parametersCount()];
                            args = new Expr[btMethod.parametersCount()];
                            LocalVar ctxParams = lbc.localVar("params", lbc.invokeInterface(
                                    MethodDesc.of(InvocationContext.class, "getParameters", Object[].class), ctx));
                            for (int i = 0; i < args.length; i++) {
                                params[i] = classDescOf(btMethod.parameterType(i));
                                args[i] = ctxParams.elem(i);
                            }
                        }
                        ClassDesc declaringClass = classDescOf(interceptedStaticMethod.getTarget());
                        String methodName = btMethod.name() + ORIGINAL_METHOD_COPY_SUFFIX;
                        MethodTypeDesc methodType = MethodTypeDesc.of(classDescOf(btMethod.returnType()), params);
                        MethodDesc targetMethod = interceptedStaticMethod.getTarget().isInterface()
                                ? InterfaceMethodDesc.of(declaringClass, methodName, methodType)
                                : ClassMethodDesc.of(declaringClass, methodName, methodType);
                        Expr ret = lbc.invokeStatic(targetMethod, args);
                        lbc.return_(ret.isVoid() ? Const.ofNull(Object.class) : ret);
                    });
                });

                // Now create metadata for the given intercepted method
                Expr metadata = b0.new_(INTERCEPTED_METHOD_METADATA_CONSTRUCTOR,
                        chain, rtMethod, rtBindings, forwardingFunc);

                // Call InterceptedStaticMethods.register()
                b0.invokeStatic(INTERCEPTED_STATIC_METHODS_REGISTER, Const.of(interceptedStaticMethod.getHash()),
                        metadata);
                b0.return_();

                // Needed when running on native image
                reflectiveMethods.produce(new ReflectiveMethodBuildItem(getClass().getName(), btMethod));
            });
        });
    }

    private Expr createInterceptorInvocation(InterceptorInfo interceptor, BlockCreator bc,
            LocalVar parentCreationalContext) {
        Expr arc = bc.invokeStatic(ARC_REQUIRE_CONTAINER);
        Expr bean = bc.invokeInterface(ARC_CONTAINER_BEAN, arc, Const.of(interceptor.getIdentifier()));
        LocalVar interceptorBean = bc.localVar("interceptor", bc.cast(bean, InjectableInterceptor.class));
        Expr creationalContext = bc.invokeStatic(CREATIONAL_CTX_CHILD, parentCreationalContext);
        Expr interceptorInstance = bc.invokeInterface(INJECTABLE_REF_PROVIDER_GET, interceptorBean,
                creationalContext);
        return bc.invokeStatic(INTERCEPTOR_INVOCATION_AROUND_INVOKE, interceptorBean, interceptorInstance);
    }

    private Expr createBindingLiteral(IndexView index, BlockCreator bc,
            AnnotationInstance binding, AnnotationLiteralProcessor annotationLiterals) {
        ClassInfo bindingClass = index.getClassByName(binding.name());
        return annotationLiterals.create(bc, bindingClass, binding);
    }

    private void generateForward(io.quarkus.gizmo2.creator.ClassCreator cc,
            InterceptedStaticMethodBuildItem interceptedStaticMethod) {
        MethodInfo method = interceptedStaticMethod.getMethod();
        cc.staticMethod(interceptedStaticMethod.getForwardingMethodName(), mc -> {
            mc.returning(classDescOf(method.returnType()));
            List<ParamVar> params = new ArrayList<>();
            for (MethodParameterInfo param : method.parameters()) {
                String name = param.name();
                if (name == null) {
                    name = "p" + param.position();
                }
                params.add(mc.parameter(name, classDescOf(param.type())));
            }
            mc.body(bc -> {
                Expr args = bc.newArray(Object.class, params);
                Expr result = bc.invokeStatic(INTERCEPTED_STATIC_METHODS_AROUND_INVOKE,
                        Const.of(interceptedStaticMethod.getHash()), args);
                bc.return_(method.returnType().kind() == Type.Kind.VOID ? Const.ofVoid() : result);
            });
        });
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

    @Record(STATIC_INIT)
    @BuildStep
    void callInitializer(BeanContainerBuildItem beanContainer, List<InterceptedStaticMethodBuildItem> interceptedStaticMethods,
            InterceptedStaticMethodsRecorder recorder) {
        if (interceptedStaticMethods.isEmpty()) {
            return;
        }
        recorder.callInitializer();
    }

}
