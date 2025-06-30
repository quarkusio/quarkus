package io.quarkus.arc.processor;

import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import jakarta.enterprise.context.spi.Contextual;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.invoke.Invoker;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.JandexReflection;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.PrimitiveType;
import org.jboss.jandex.Type;
import org.jboss.jandex.TypeVariable;
import org.jboss.logging.Logger;
import org.objectweb.asm.Opcodes;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.impl.CreationalContextImpl;
import io.quarkus.arc.impl.InvokerCleanupTasks;
import io.quarkus.arc.processor.BuiltinBean.GeneratorContext;
import io.quarkus.arc.processor.ResourceOutput.Resource;
import io.quarkus.gizmo.AssignableResultHandle;
import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.CatchBlockCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FieldCreator;
import io.quarkus.gizmo.Gizmo;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.gizmo.TryBlock;
import io.smallrye.common.annotation.SuppressForbidden;

public class InvokerGenerator extends AbstractGenerator {
    private static final Logger LOGGER = Logger.getLogger(InvokerGenerator.class);

    private final Predicate<DotName> applicationClassPredicate;
    private final IndexView beanArchiveIndex;
    private final BeanDeployment beanDeployment;
    private final AnnotationLiteralProcessor annotationLiterals;
    private final ReflectionRegistration reflectionRegistration;
    private final Predicate<DotName> injectionPointAnnotationsPredicate;

    private final Assignability assignability;

    InvokerGenerator(boolean generateSources, Predicate<DotName> applicationClassPredicate, BeanDeployment deployment,
            AnnotationLiteralProcessor annotationLiterals, ReflectionRegistration reflectionRegistration,
            Predicate<DotName> injectionPointAnnotationsPredicate) {
        super(generateSources);
        this.applicationClassPredicate = applicationClassPredicate;
        this.beanArchiveIndex = deployment.getBeanArchiveIndex();
        this.beanDeployment = deployment;
        this.annotationLiterals = annotationLiterals;
        this.reflectionRegistration = reflectionRegistration;
        this.injectionPointAnnotationsPredicate = injectionPointAnnotationsPredicate;

        this.assignability = new Assignability(deployment.getBeanArchiveIndex());
    }

    Collection<Resource> generate(InvokerInfo invoker) {
        Function<String, Resource.SpecialType> specialTypeFunction = className -> {
            if (className.equals(invoker.className) || className.equals(invoker.wrapperClassName)) {
                return Resource.SpecialType.INVOKER;
            }
            return null;
        };

        ResourceClassOutput classOutput = new ResourceClassOutput(
                applicationClassPredicate.test(invoker.targetBeanClass.name()), specialTypeFunction, generateSources);

        createInvokerClass(classOutput, invoker);
        createInvokerWrapperClass(classOutput, invoker);
        createInvokerLazyClass(classOutput, invoker);

        return classOutput.getResources();
    }

    // ---

    private void createInvokerLazyClass(ClassOutput classOutput, InvokerInfo invoker) {
        if (!invoker.usesLookup) {
            return;
        }

        try (ClassCreator clazz = ClassCreator.builder()
                .classOutput(classOutput)
                .className(invoker.lazyClassName)
                .interfaces(Invoker.class)
                .build()) {

            String invokerClass = invoker.wrapperClassName != null ? invoker.wrapperClassName : invoker.className;

            MethodCreator invoke = clazz.getMethodCreator("invoke", Object.class, Object.class, Object[].class);
            ResultHandle delegateInvoker = invoke.invokeStaticMethod(
                    MethodDescriptor.ofMethod(invokerClass, "get", Invoker.class));
            ResultHandle result = invoke.invokeInterfaceMethod(
                    MethodDescriptor.ofMethod(Invoker.class, "invoke", Object.class, Object.class, Object[].class),
                    delegateInvoker, invoke.getMethodParam(0), invoke.getMethodParam(1));
            invoke.returnValue(result);

            LOGGER.debugf("LazyInvoker class generated: %s", clazz.getClassName());
        }
    }

    // ---

    private void createInvokerWrapperClass(ClassOutput classOutput, InvokerInfo invoker) {
        if (invoker.wrapperClassName == null) {
            return;
        }

        try (ClassCreator clazz = ClassCreator.builder()
                .classOutput(classOutput)
                .className(invoker.wrapperClassName)
                .interfaces(Invoker.class)
                .build()) {

            FieldCreator delegate = clazz.getFieldCreator("delegate", Invoker.class)
                    .setModifiers(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL);

            MethodCreator ctor = clazz.getMethodCreator(Methods.INIT, void.class)
                    .setModifiers(Opcodes.ACC_PUBLIC);
            ctor.invokeSpecialMethod(MethodDescriptor.ofMethod(Object.class, Methods.INIT, void.class), ctor.getThis());
            ctor.writeInstanceField(delegate.getFieldDescriptor(), ctor.getThis(),
                    ctor.invokeStaticMethod(MethodDescriptor.ofMethod(invoker.className, "get", Invoker.class)));
            ctor.returnVoid();

            MethodCreator invoke = clazz.getMethodCreator("invoke", Object.class, Object.class, Object[].class);
            ResultHandle targetInstance = invoke.getMethodParam(0);
            ResultHandle argumentsArray = invoke.getMethodParam(1);
            ResultHandle delegateInvoker = invoke.readInstanceField(delegate.getFieldDescriptor(), invoke.getThis());
            MethodInfo wrappingMethod = findWrapper(invoker);
            ResultHandle result = invoker.invocationWrapper.clazz.isInterface()
                    ? invoke.invokeStaticInterfaceMethod(wrappingMethod, targetInstance, argumentsArray, delegateInvoker)
                    : invoke.invokeStaticMethod(wrappingMethod, targetInstance, argumentsArray, delegateInvoker);
            if (wrappingMethod.returnType().kind() == Type.Kind.VOID) {
                result = invoke.loadNull();
            }
            invoke.returnValue(result);

            generateStaticGetMethod(clazz, ctor);

            LOGGER.debugf("InvokerWrapper class generated: %s", clazz.getClassName());
        }
    }

    private MethodInfo findWrapper(InvokerInfo invoker) {
        InvocationTransformer wrapper = invoker.invocationWrapper;
        ClassInfo clazz = beanArchiveIndex.getClassByName(wrapper.clazz);
        List<MethodInfo> methods = new ArrayList<>();
        for (MethodInfo method : clazz.methods()) {
            if (Modifier.isStatic(method.flags()) && wrapper.method.equals(method.name())) {
                methods.add(method);
            }
        }

        List<MethodInfo> matching = new ArrayList<>();
        List<MethodInfo> notMatching = new ArrayList<>();
        for (MethodInfo method : methods) {
            if (method.parametersCount() == 3
                    && method.parameterType(1).kind() == Type.Kind.ARRAY
                    && method.parameterType(1).asArrayType().deepDimensions() == 1
                    && method.parameterType(1).asArrayType().elementType().name().equals(DotName.OBJECT_NAME)
                    && method.parameterType(2).name().equals(DotName.createSimple(Invoker.class))) {

                Type targetInstanceType = method.parameterType(0);
                boolean targetInstanceOk = isAnyType(targetInstanceType)
                        || assignability.isSupertype(targetInstanceType, ClassType.create(invoker.targetBeanClass.name()));

                boolean isInvokerRaw = method.parameterType(2).kind() == Type.Kind.CLASS;
                boolean isInvokerParameterized = method.parameterType(2).kind() == Type.Kind.PARAMETERIZED_TYPE
                        && method.parameterType(2).asParameterizedType().arguments().size() == 2;
                boolean invokerTargetInstanceOk = isInvokerRaw
                        || isInvokerParameterized
                                && targetInstanceType.equals(method.parameterType(2).asParameterizedType().arguments().get(0));

                if (targetInstanceOk && invokerTargetInstanceOk) {
                    matching.add(method);
                } else {
                    notMatching.add(method);
                }
            } else {
                notMatching.add(method);
            }
        }

        if (matching.size() == 1) {
            return matching.get(0);
        }

        if (matching.isEmpty()) {
            String expectation = ""
                    + "\tmatching methods must be `static` and take 3 parameters (instance, argument array, invoker)\n"
                    + "\tthe 1st parameter must be a supertype of " + invoker.targetBeanClass.name() + ", possibly Object\n"
                    + "\tthe 2nd parameter must be Object[]\n"
                    + "\tthe 3rd parameter must be Invoker<type of 1st parameter, some type>";
            if (notMatching.isEmpty()) {
                throw new IllegalArgumentException(""
                        + "Error creating invoker for method " + invoker + ":\n"
                        + "\tno matching method found for " + wrapper + "\n"
                        + expectation);
            } else {
                throw new IllegalArgumentException(""
                        + "Error creating invoker for method " + invoker + ":\n"
                        + "\tno matching method found for " + wrapper + "\n"
                        + "\tfound methods that do not match:\n"
                        + notMatching.stream().map(it -> "\t- " + it).collect(Collectors.joining("\n")) + "\n"
                        + expectation);
            }
        } else {
            throw new IllegalArgumentException(""
                    + "Error creating invoker for method " + invoker + ":\n"
                    + "\ttoo many matching methods for " + wrapper + ":\n"
                    + matching.stream().map(it -> "\t- " + it).collect(Collectors.joining("\n")));
        }
    }

    // ---

    private void createInvokerClass(ClassOutput classOutput, InvokerInfo invoker) {
        MethodInfo targetMethod = invoker.method;

        try (ClassCreator clazz = ClassCreator.builder()
                .classOutput(classOutput)
                .className(invoker.className)
                .interfaces(Invoker.class)
                .build()) {

            FieldCreator instance = clazz.getFieldCreator("INSTANCE", AtomicReference.class)
                    .setModifiers(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL);

            MethodCreator clinit = clazz.getMethodCreator(Methods.CLINIT, void.class)
                    .setModifiers(Opcodes.ACC_STATIC);
            clinit.writeStaticField(instance.getFieldDescriptor(),
                    clinit.newInstance(MethodDescriptor.ofConstructor(AtomicReference.class)));
            clinit.returnVoid();

            MethodCreator ctor = clazz.getMethodCreator(Methods.INIT, void.class)
                    .setModifiers(Opcodes.ACC_PUBLIC);
            ctor.invokeSpecialMethod(MethodDescriptor.ofMethod(Object.class, Methods.INIT, void.class), ctor.getThis());

            MethodCreator invoke = clazz.getMethodCreator("invoke", Object.class, Object.class, Object[].class);

            FinisherGenerator finisher = new FinisherGenerator(invoke);
            LookupGenerator lookup = prepareLookup(invoke, invoker, ctor, clazz, classOutput);

            ResultHandle targetInstance = null;
            if (!Modifier.isStatic(targetMethod.flags())) {
                Type instanceType = ClassType.create(invoker.targetBeanClass.name());
                targetInstance = invoker.instanceLookup
                        ? lookup.targetBeanInstance()
                        : invoke.getMethodParam(0);
                targetInstance = findAndInvokeTransformer(invoker.instanceTransformer, instanceType,
                        invoker, targetInstance, invoke, finisher);
            }

            ResultHandle argumentsArray = invoke.getMethodParam(1);
            ResultHandle[] unfoldedArguments = new ResultHandle[targetMethod.parametersCount()];
            for (int i = 0; i < targetMethod.parametersCount(); i++) {
                Type parameterType = targetMethod.parameterType(i);
                ResultHandle originalArgument = invoker.argumentLookups[i]
                        ? lookup.argumentBeanInstance(i)
                        : invoke.readArrayValue(argumentsArray, i);
                originalArgument = findAndInvokeTransformer(invoker.argumentTransformers[i], parameterType,
                        invoker, originalArgument, invoke, finisher);
                if (parameterType.kind() == Type.Kind.PRIMITIVE) {
                    // if the transformer returned a primitive type, box it
                    Type transformedType = transformerReturnType(invoker.argumentTransformers[i], parameterType, invoker);
                    if (transformedType != null && transformedType.kind() == Type.Kind.PRIMITIVE) {
                        originalArgument = invoke.checkCast(originalArgument,
                                PrimitiveType.box(parameterType.asPrimitiveType()).name().toString());
                    }

                    BranchResult ifNotNull = invoke.ifNotNull(originalArgument);
                    AssignableResultHandle variable = invoke.createVariable(parameterType.descriptor());
                    assignWithUnboxingAndWideningConversion(ifNotNull.trueBranch(), originalArgument, variable,
                            parameterType.asPrimitiveType(), i);
                    originalArgument = variable;
                    ifNotNull.falseBranch().throwException(NullPointerException.class,
                            "Argument " + i + " is null, " + parameterType + " expected");
                }
                unfoldedArguments[i] = originalArgument;
            }

            // lookups need to add to the constructor, so we need to finish it here
            ctor.returnVoid();

            if (lookup != null && invoker.argumentLookups.length > 0) {
                // the specification requires that the arguments array has at least as many elements
                // as the target method has parameters, even if some of them (or all of them)
                // are looked up
                //
                // we check that by simply reading the last parameter's position in the arguments array, when:
                // 1. some lookups are configured (otherwise the check would be duplicate)
                // 2. the target method has parameters (otherwise the check would be meaningless)
                invoke.readArrayValue(argumentsArray, invoker.argumentLookups.length - 1);
            }

            TryBlock tryBlock = invoke.tryBlock();
            CatchBlockCreator catchBlock = tryBlock.addCatch(Throwable.class);

            if (finisher.wasCreated()) {
                catchBlock.invokeVirtualMethod(MethodDescriptor.ofMethod(InvokerCleanupTasks.class, "finish", void.class),
                        finisher.getOrCreate());
            }
            if (lookup != null) {
                lookup.destroyIfNecessary(catchBlock, null);
            }

            if (invoker.exceptionTransformer != null) {
                catchBlock.returnValue(
                        findAndInvokeTransformer(invoker.exceptionTransformer, ClassType.create(Throwable.class),
                                invoker, catchBlock.getCaughtException(), catchBlock, null));
            } else {
                catchBlock.throwException(catchBlock.getCaughtException());
            }

            ResultHandle result;
            boolean isInterface = invoker.method.declaringClass().isInterface();
            if (Modifier.isStatic(targetMethod.flags())) {
                result = isInterface
                        ? tryBlock.invokeStaticInterfaceMethod(targetMethod, unfoldedArguments)
                        : tryBlock.invokeStaticMethod(targetMethod, unfoldedArguments);
            } else {
                result = isInterface
                        ? tryBlock.invokeInterfaceMethod(targetMethod, targetInstance, unfoldedArguments)
                        : tryBlock.invokeVirtualMethod(targetMethod, targetInstance, unfoldedArguments);
            }
            if (targetMethod.returnType().kind() == Type.Kind.VOID) {
                result = tryBlock.loadNull();
            }
            if (lookup != null) {
                result = lookup.destroyIfNecessary(tryBlock, result);
            }
            result = findAndInvokeTransformer(invoker.returnValueTransformer, targetMethod.returnType(),
                    invoker, result, tryBlock, null);
            if (finisher.wasCreated()) {
                tryBlock.invokeVirtualMethod(MethodDescriptor.ofMethod(InvokerCleanupTasks.class, "finish", void.class),
                        finisher.getOrCreate());
            }
            tryBlock.returnValue(result);

            generateStaticGetMethod(clazz, ctor);

            LOGGER.debugf("Invoker class generated: %s", clazz.getClassName());
        }
    }

    private static void generateStaticGetMethod(ClassCreator clazz, MethodCreator ctor) {
        FieldCreator instance = clazz.getFieldCreator("INSTANCE", AtomicReference.class)
                .setModifiers(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL);

        MethodCreator clinit = clazz.getMethodCreator(Methods.CLINIT, void.class)
                .setModifiers(Opcodes.ACC_STATIC);
        clinit.writeStaticField(instance.getFieldDescriptor(),
                clinit.newInstance(MethodDescriptor.ofConstructor(AtomicReference.class)));
        clinit.returnVoid();

        MethodCreator get = clazz.getMethodCreator("get", Invoker.class).setModifiers(Modifier.PUBLIC | Modifier.STATIC);
        ResultHandle atomicReference = get.readStaticField(instance.getFieldDescriptor());
        AssignableResultHandle resultInstance = get.createVariable(Invoker.class);
        get.assign(resultInstance, get.invokeVirtualMethod(
                MethodDescriptor.ofMethod(AtomicReference.class, "get", Object.class), atomicReference));
        BytecodeCreator isNull = get.ifNotNull(resultInstance).falseBranch();
        isNull.invokeVirtualMethod(
                MethodDescriptor.ofMethod(AtomicReference.class, "compareAndSet", boolean.class, Object.class, Object.class),
                atomicReference, isNull.loadNull(), isNull.newInstance(ctor.getMethodDescriptor()));
        isNull.assign(resultInstance, isNull.invokeVirtualMethod(
                MethodDescriptor.ofMethod(AtomicReference.class, "get", Object.class), atomicReference));
        get.returnValue(resultInstance);
    }

    private static void assignWithUnboxingAndWideningConversion(BytecodeCreator bytecode, ResultHandle value,
            AssignableResultHandle target, PrimitiveType targetType, int argumentNumber) {

        // unboxing conversion
        {
            ClassType possibleSourceType = PrimitiveType.box(targetType);
            ResultHandle isInstance = bytecode.instanceOf(value, possibleSourceType.name().toString());
            BranchResult ifNotInstanceOf = bytecode.ifFalse(isInstance);
            ifNotInstanceOf.falseBranch().assign(target, value); // Gizmo emits unboxing conversion automatically
            bytecode = ifNotInstanceOf.trueBranch();
        }
        // widening conversions
        for (ClassType possibleSourceType : WIDENING_CONVERSIONS_TO.get(targetType)) {
            ResultHandle isInstance = bytecode.instanceOf(value, possibleSourceType.name().toString());
            BranchResult ifNotInstanceOf = bytecode.ifFalse(isInstance);
            BytecodeCreator unbox = ifNotInstanceOf.falseBranch();
            AssignableResultHandle unboxed = unbox.createVariable(PrimitiveType.unbox(possibleSourceType).descriptor());
            unbox.assign(unboxed, value); // Gizmo emits unboxing conversion automatically
            ResultHandle widened = unbox.convertPrimitive(unboxed, JandexReflection.loadRawType(targetType));
            unbox.assign(target, widened);
            bytecode = ifNotInstanceOf.trueBranch();
        }

        ResultHandle message = Gizmo.newStringBuilder(bytecode)
                .append("No method invocation conversion to ")
                .append(targetType.name().toString())
                .append(" exists for argument ")
                .append("" + argumentNumber)
                .append(": ")
                .append(bytecode.invokeVirtualMethod(MethodDescriptors.OBJECT_GET_CLASS, value))
                .append(", value ")
                .append(value)
                .callToString();
        ResultHandle exception = bytecode.newInstance(
                MethodDescriptor.ofConstructor(ClassCastException.class, String.class), message);
        bytecode.throwException(exception);
    }

    private static final Map<PrimitiveType, Set<ClassType>> WIDENING_CONVERSIONS_TO = Map.of(
            PrimitiveType.BOOLEAN, Set.of(),
            PrimitiveType.BYTE, Set.of(),
            PrimitiveType.SHORT, Set.of(ClassType.BYTE_CLASS),
            PrimitiveType.INT, Set.of(ClassType.BYTE_CLASS, ClassType.SHORT_CLASS, ClassType.CHARACTER_CLASS),
            PrimitiveType.LONG, Set.of(ClassType.BYTE_CLASS, ClassType.SHORT_CLASS, ClassType.INTEGER_CLASS,
                    ClassType.CHARACTER_CLASS),
            PrimitiveType.FLOAT, Set.of(ClassType.BYTE_CLASS, ClassType.SHORT_CLASS, ClassType.INTEGER_CLASS,
                    ClassType.LONG_CLASS, ClassType.CHARACTER_CLASS),
            PrimitiveType.DOUBLE, Set.of(ClassType.BYTE_CLASS, ClassType.SHORT_CLASS, ClassType.INTEGER_CLASS,
                    ClassType.LONG_CLASS, ClassType.FLOAT_CLASS, ClassType.CHARACTER_CLASS),
            PrimitiveType.CHAR, Set.of());

    static class FinisherGenerator {
        private final MethodCreator method;
        private ResultHandle finisher;

        FinisherGenerator(MethodCreator method) {
            this.method = method;
        }

        ResultHandle getOrCreate() {
            if (finisher == null) {
                finisher = method.newInstance(MethodDescriptor.ofConstructor(InvokerCleanupTasks.class));
            }
            return finisher;
        }

        boolean wasCreated() {
            return finisher != null;
        }
    }

    static class ResolvedBean {
        // exactly one of these is non-null
        private final BeanInfo userBean;
        private final BuiltinBean builtinBean;

        static ResolvedBean of(BeanInfo userBean) {
            return new ResolvedBean(userBean, null);
        }

        static ResolvedBean of(InjectionPointInfo injectionPoint) {
            BeanInfo userBean = injectionPoint.getResolvedBean();
            if (userBean != null) {
                return new ResolvedBean(userBean, null);
            }
            BuiltinBean builtinBean = BuiltinBean.resolve(injectionPoint);
            if (builtinBean != null) {
                return new ResolvedBean(null, builtinBean);
            }
            throw new IllegalStateException("Injection point not resolved: " + injectionPoint);
        }

        private ResolvedBean(BeanInfo userBean, BuiltinBean builtinBean) {
            this.userBean = userBean;
            this.builtinBean = builtinBean;
        }

        boolean isUserBean() {
            return userBean != null;
        }

        boolean isBuiltinBean() {
            return builtinBean != null;
        }

        BeanInfo getUserBean() {
            assert isUserBean();
            return userBean;
        }

        BuiltinBean getBuiltinBean() {
            assert isBuiltinBean();
            return builtinBean;
        }

        boolean requiresDestruction() {
            return userBean != null && BuiltinScope.DEPENDENT.is(userBean.getScope());
        }
    }

    static class LookupGenerator {
        private final BeanDeployment beanDeployment;
        private final AnnotationLiteralProcessor annotationLiterals;
        private final ReflectionRegistration reflectionRegistration;
        private final Predicate<DotName> injectionPointAnnotationsPredicate;

        private final InvokerInfo invoker;

        private final ResolvedBean targetBean;
        private final ResolvedBean[] argumentBeans;

        private final MethodCreator invokeMethod;
        private final MethodCreator invokerConstructor;
        private final ClassCreator invokerClass;
        private final ClassOutput classOutput;

        // in constructor
        private ResultHandle arc;

        // in `invoke()`
        private ResultHandle rootCreationalContext;

        LookupGenerator(BeanDeployment beanDeployment, AnnotationLiteralProcessor annotationLiterals,
                ReflectionRegistration reflectionRegistration, Predicate<DotName> injectionPointAnnotationsPredicate,
                InvokerInfo invoker, ResolvedBean targetBean, ResolvedBean[] argumentBeans, MethodCreator invokeMethod,
                MethodCreator invokerConstructor, ClassCreator invokerClass, ClassOutput classOutput) {
            this.beanDeployment = beanDeployment;
            this.annotationLiterals = annotationLiterals;
            this.reflectionRegistration = reflectionRegistration;
            this.injectionPointAnnotationsPredicate = injectionPointAnnotationsPredicate;
            this.invoker = invoker;

            this.targetBean = targetBean;
            this.argumentBeans = argumentBeans;

            this.invokeMethod = invokeMethod;
            this.invokerConstructor = invokerConstructor;
            this.invokerClass = invokerClass;
            this.classOutput = classOutput;
        }

        ResultHandle arc() {
            if (arc == null) {
                arc = invokerConstructor.invokeStaticMethod(
                        MethodDescriptor.ofMethod(Arc.class, "container", ArcContainer.class));
            }
            return arc;
        }

        ResultHandle rootCreationalContext() {
            if (rootCreationalContext == null) {
                rootCreationalContext = invokeMethod.newInstance(
                        MethodDescriptor.ofConstructor(CreationalContextImpl.class, Contextual.class), invokeMethod.loadNull());
            }
            return rootCreationalContext;
        }

        // expected to be called at most once
        ResultHandle targetBeanInstance() {
            String name = "target";
            FieldCreator field = invokerClass.getFieldCreator(name, Supplier.class)
                    .setModifiers(Modifier.PRIVATE | Modifier.FINAL);

            // in constructor
            ResultHandle bean = invokerConstructor.invokeInterfaceMethod(
                    MethodDescriptor.ofMethod(ArcContainer.class, "bean", InjectableBean.class, String.class),
                    arc(), invokerConstructor.load(targetBean.getUserBean().getIdentifier()));
            invokerConstructor.writeInstanceField(field.getFieldDescriptor(), invokerConstructor.getThis(), bean);

            // in `invoke()`
            ResultHandle supplier = invokeMethod.readInstanceField(field.getFieldDescriptor(), invokeMethod.getThis());
            ResultHandle injectableReferenceProvider = invokeMethod.invokeInterfaceMethod(
                    MethodDescriptors.SUPPLIER_GET, supplier);
            ResultHandle creationalContext = invokeMethod.invokeStaticMethod(
                    MethodDescriptors.CREATIONAL_CTX_CHILD_CONTEXTUAL, injectableReferenceProvider, rootCreationalContext());
            return invokeMethod.invokeInterfaceMethod(
                    MethodDescriptors.INJECTABLE_REF_PROVIDER_GET, injectableReferenceProvider, creationalContext);
        }

        // expected to be called at most once for each position
        ResultHandle argumentBeanInstance(int position) {
            String name = "arg" + position;
            FieldCreator field = invokerClass.getFieldCreator(name, Supplier.class)
                    .setModifiers(Modifier.PRIVATE | Modifier.FINAL);

            // in constructor
            ResolvedBean resolved = argumentBeans[position];
            if (resolved.isUserBean()) {
                ResultHandle bean = invokerConstructor.invokeInterfaceMethod(
                        MethodDescriptor.ofMethod(ArcContainer.class, "bean", InjectableBean.class, String.class),
                        arc(), invokerConstructor.load(resolved.getUserBean().getIdentifier()));
                invokerConstructor.writeInstanceField(field.getFieldDescriptor(), invokerConstructor.getThis(), bean);
            } else {
                BuiltinBean builtinBean = resolved.getBuiltinBean();
                InjectionPointInfo injectionPoint = invoker.getInjectionPointForArgument(position);
                builtinBean.getGenerator().generate(new GeneratorContext(classOutput, beanDeployment, injectionPoint,
                        invokerClass, invokerConstructor, name, annotationLiterals, invoker, reflectionRegistration,
                        injectionPointAnnotationsPredicate));
            }

            // in `invoke()`
            ResultHandle supplier = invokeMethod.readInstanceField(field.getFieldDescriptor(), invokeMethod.getThis());
            ResultHandle injectableReferenceProvider = invokeMethod.invokeInterfaceMethod(
                    MethodDescriptors.SUPPLIER_GET, supplier);
            ResultHandle creationalContext = invokeMethod.invokeStaticMethod(
                    MethodDescriptors.CREATIONAL_CTX_CHILD_CONTEXTUAL, injectableReferenceProvider, rootCreationalContext());
            return invokeMethod.invokeInterfaceMethod(
                    MethodDescriptors.INJECTABLE_REF_PROVIDER_GET, injectableReferenceProvider, creationalContext);
        }

        ResultHandle destroyIfNecessary(BytecodeCreator bytecode, ResultHandle returnValue) {
            if (rootCreationalContext == null) {
                return returnValue;
            }

            // `returnValue` is `null` when the target method has thrown an exception;
            // we're going to rethrow it, so we need to release the `CreationalContext` immediately
            if (returnValue != null && invoker.isAsynchronous()) {
                String asyncType = invoker.method.returnType().name().toString();
                return bytecode.invokeStaticMethod(MethodDescriptor.ofMethod(InvokerCleanupTasks.class,
                        "deferRelease", asyncType, CreationalContext.class, asyncType),
                        rootCreationalContext(), returnValue);
            } else {
                bytecode.invokeInterfaceMethod(MethodDescriptors.CREATIONAL_CTX_RELEASE, rootCreationalContext());
                return returnValue;
            }
        }
    }

    private LookupGenerator prepareLookup(MethodCreator invokeMethod, InvokerInfo invoker, MethodCreator invokerConstructor,
            ClassCreator invokerClass, ClassOutput classOutput) {
        boolean lookupUsed = invoker.instanceLookup;
        for (boolean argumentLookup : invoker.argumentLookups) {
            lookupUsed |= argumentLookup;
        }

        if (!lookupUsed) {
            return null;
        }

        ResolvedBean targetInstance = null;
        if (invoker.instanceLookup) {
            targetInstance = ResolvedBean.of(invoker.targetBean);
        }

        ResolvedBean[] arguments = new ResolvedBean[invoker.argumentLookups.length];
        for (int i = 0; i < invoker.argumentLookups.length; i++) {
            if (invoker.argumentLookups[i]) {
                InjectionPointInfo injectionPoint = invoker.getInjectionPointForArgument(i);
                if (injectionPoint != null) {
                    arguments[i] = ResolvedBean.of(injectionPoint);
                } else {
                    throw new IllegalStateException("No injection point for argument " + i + " of " + invoker);
                }
            }
        }

        return new LookupGenerator(beanDeployment, annotationLiterals, reflectionRegistration,
                injectionPointAnnotationsPredicate, invoker, targetInstance, arguments, invokeMethod,
                invokerConstructor, invokerClass, classOutput);
    }

    private ResultHandle findAndInvokeTransformer(InvocationTransformer transformer, Type expectedType,
            InvokerInfo invoker, ResultHandle originalValue, BytecodeCreator bytecode, FinisherGenerator finisher) {
        if (transformer == null) {
            return originalValue;
        }

        CandidateMethods candidates = findCandidates(transformer, expectedType, invoker);
        CandidateMethod transformerMethod = candidates.resolve();
        MethodDescriptor transformerMethodDescriptor = MethodDescriptor.of(transformerMethod.method);

        if (Modifier.isStatic(transformerMethod.method.flags())) {
            ResultHandle[] arguments = new ResultHandle[transformerMethod.usesFinisher() ? 2 : 1];
            arguments[0] = originalValue;
            if (transformerMethod.usesFinisher()) {
                arguments[1] = finisher.getOrCreate();
            }

            if (transformer.clazz.isInterface()) {
                return bytecode.invokeStaticInterfaceMethod(transformerMethodDescriptor, arguments);
            } else {
                return bytecode.invokeStaticMethod(transformerMethodDescriptor, arguments);
            }
        } else {
            if (transformer.clazz.isInterface()) {
                return bytecode.invokeInterfaceMethod(transformerMethodDescriptor, originalValue);
            } else {
                return bytecode.invokeVirtualMethod(transformerMethodDescriptor, originalValue);
            }
        }
    }

    private Type transformerReturnType(InvocationTransformer transformer, Type expectedType, InvokerInfo invoker) {
        if (transformer == null) {
            return null;
        }

        CandidateMethods candidates = findCandidates(transformer, expectedType, invoker);
        CandidateMethod transformerMethod = candidates.resolve();
        return transformerMethod.method.returnType();
    }

    private CandidateMethods findCandidates(InvocationTransformer transformer, Type expectedType, InvokerInfo invoker) {
        assert transformer.kind != InvocationTransformerKind.WRAPPER;

        ClassInfo clazz = beanArchiveIndex.getClassByName(transformer.clazz);

        // static methods only from the given class
        // instance methods also from superclasses and superinterfaces

        // first, set up the worklist so that it contains the given class and all its superclasses
        // next, as each class from the queue is processed, add its interfaces to the queue
        // this is so that superclasses are processed before interfaces
        Deque<ClassInfo> worklist = new ArrayDeque<>();
        while (clazz != null) {
            worklist.addLast(clazz);
            clazz = clazz.superName() == null ? null : beanArchiveIndex.getClassByName(clazz.superName());
        }

        boolean originalClass = true;
        Set<Methods.MethodKey> seenMethods = new HashSet<>();
        while (!worklist.isEmpty()) {
            ClassInfo current = worklist.removeFirst();

            for (MethodInfo method : current.methods()) {
                if (!transformer.method.equals(method.name())) {
                    continue;
                }

                Methods.MethodKey key = new Methods.MethodKey(method);

                if (Modifier.isStatic(method.flags()) && originalClass) {
                    seenMethods.add(key);
                } else {
                    if (!Methods.isOverriden(key, seenMethods)) {
                        seenMethods.add(key);
                    }
                }
            }

            for (DotName iface : current.interfaceNames()) {
                worklist.addLast(beanArchiveIndex.getClassByName(iface));
            }

            originalClass = false;
        }

        List<CandidateMethod> matching = new ArrayList<>();
        List<CandidateMethod> notMatching = new ArrayList<>();
        for (Methods.MethodKey seenMethod : seenMethods) {
            CandidateMethod candidate = new CandidateMethod(seenMethod.method, assignability);
            if (candidate.matches(transformer, expectedType)) {
                matching.add(candidate);
            } else {
                notMatching.add(candidate);
            }
        }
        return new CandidateMethods(transformer, expectedType, matching, notMatching, invoker);
    }

    static class CandidateMethods {
        // most of the fields here are only used for providing a good error message
        final InvocationTransformer transformer;
        final Type expectedType;
        final List<CandidateMethod> matching;
        final List<CandidateMethod> notMatching;
        final InvokerInfo invoker;

        CandidateMethods(InvocationTransformer transformer, Type expectedType,
                List<CandidateMethod> matching, List<CandidateMethod> notMatching,
                InvokerInfo invoker) {
            this.transformer = transformer;
            this.expectedType = expectedType;
            this.matching = matching;
            this.notMatching = notMatching;
            this.invoker = invoker;
        }

        @SuppressForbidden(reason = "Using Type.toString() to build an informative message")
        CandidateMethod resolve() {
            if (matching.size() == 1) {
                return matching.get(0);
            }

            if (matching.isEmpty()) {
                String expectedType = this.expectedType.toString();
                String expectation = "";
                if (transformer.isInputTransformer()) {
                    expectation = "\n"
                            + "\tmatching `static` methods must take 1 or 2 parameters and return " + expectedType
                            + " (or subtype)\n"
                            + "\t(if the `static` method takes 2 parameters, the 2nd must be `Consumer<Runnable>`)\n"
                            + "\tmatching instance methods must take no parameter and return " + expectedType + " (or subtype)";
                } else if (transformer.isOutputTransformer()) {
                    expectation = "\n"
                            + "\tmatching `static` method must take 1 parameter of type " + expectedType + " (or supertype)\n"
                            + "\tmatching instance methods must be declared on " + expectedType
                            + " (or supertype) and take no parameter";
                }

                if (notMatching.isEmpty()) {
                    throw new IllegalArgumentException(""
                            + "Error creating invoker for method " + invoker + ":\n"
                            + "\tno matching method found for " + transformer
                            + expectation);
                } else {
                    throw new IllegalArgumentException(""
                            + "Error creating invoker for method " + invoker + ":\n"
                            + "\tno matching method found for " + transformer + "\n"
                            + "\tfound methods that do not match:\n"
                            + notMatching.stream().map(it -> "\t- " + it).collect(Collectors.joining("\n"))
                            + expectation);
                }
            } else {
                throw new IllegalArgumentException(""
                        + "Error creating invoker for method " + invoker + ":\n"
                        + "\ttoo many matching methods for " + transformer + ":\n"
                        + matching.stream().map(it -> "\t- " + it).collect(Collectors.joining("\n")));
            }
        }
    }

    static class CandidateMethod {
        final MethodInfo method;
        final Assignability assignability;

        CandidateMethod(MethodInfo method, Assignability assignability) {
            this.method = method;
            this.assignability = assignability;
        }

        boolean matches(InvocationTransformer transformer, Type expectedType) {
            if (transformer.isInputTransformer()) {
                // for input transformer (target instance, argument):
                // - we can't check what comes into the transformer
                // - we can check what comes out of the transformer, because that's what the invokable method consumes
                //   (and the transformer must produce a subtype)

                boolean returnTypeOk = isAnyType(method.returnType()) || isSubtype(method.returnType(), expectedType);
                if (Modifier.isStatic(method.flags())) {
                    return method.parametersCount() == 1 && returnTypeOk
                            || method.parametersCount() == 2 && returnTypeOk && isFinisher(method.parameterType(1));
                } else {
                    return method.parametersCount() == 0 && returnTypeOk;
                }
            } else if (transformer.isOutputTransformer()) {
                // for output transformer (return value, exception):
                // - we can check what comes into the transformer, because that's what the invokable method produces
                //   (and the transformer must consume a supertype)
                // - we can't check what comes out of the transformer

                if (Modifier.isStatic(method.flags())) {
                    return method.parametersCount() == 1
                            && (isAnyType(method.parameterType(0)) || isSupertype(method.parameterType(0), expectedType));
                } else {
                    return method.parametersCount() == 0
                            && isSupertype(ClassType.create(method.declaringClass().name()), expectedType);
                }
            } else {
                throw new IllegalArgumentException(transformer.toString());
            }
        }

        // if `matches()` returns `false`, there's no point in calling this method
        boolean usesFinisher() {
            return Modifier.isStatic(method.flags())
                    && method.parametersCount() == 2
                    && isFinisher(method.parameterType(1));
        }

        private boolean isFinisher(Type type) {
            if (type.kind() == Type.Kind.CLASS) {
                return type.name().equals(DotName.createSimple(Consumer.class));
            } else if (type.kind() == Type.Kind.PARAMETERIZED_TYPE) {
                return type.name().equals(DotName.createSimple(Consumer.class))
                        && type.asParameterizedType().arguments().size() == 1
                        && type.asParameterizedType().arguments().get(0).kind() == Type.Kind.CLASS
                        && type.asParameterizedType().arguments().get(0).name().equals(DotName.createSimple(Runnable.class));
            } else {
                return false;
            }
        }

        private boolean isSubtype(Type a, Type b) {
            return assignability.isSubtype(a, b);
        }

        private boolean isSupertype(Type a, Type b) {
            return assignability.isSupertype(a, b);
        }

        @Override
        public String toString() {
            return method.toString() + " declared on " + method.declaringClass();
        }
    }

    // ---

    static boolean isAnyType(Type t) {
        if (ClassType.OBJECT_TYPE.equals(t)) {
            return true;
        }
        if (t.kind() == Type.Kind.TYPE_VARIABLE) {
            TypeVariable typeVar = t.asTypeVariable();
            return typeVar.bounds().isEmpty() || isAnyType(typeVar.bounds().get(0));
        }
        return false;
    }

    // this is mostly a prototype, doesn't follow any specification
    static class Assignability {
        private final AssignabilityCheck assignabilityCheck;

        Assignability(IndexView index) {
            this.assignabilityCheck = new AssignabilityCheck(index, null);
        }

        boolean isSubtype(Type a, Type b) {
            Objects.requireNonNull(a);
            Objects.requireNonNull(b);

            switch (a.kind()) {
                case VOID:
                    return b.kind() == Type.Kind.VOID
                            || b.kind() == Type.Kind.CLASS && b.asClassType().name().equals(DotName.createSimple(Void.class));
                case PRIMITIVE:
                    return b.kind() == Type.Kind.PRIMITIVE
                            && a.asPrimitiveType().primitive() == b.asPrimitiveType().primitive();
                case ARRAY:
                    return b.kind() == Type.Kind.ARRAY
                            && a.asArrayType().deepDimensions() == b.asArrayType().deepDimensions()
                            && isSubtype(a.asArrayType().elementType(), b.asArrayType().elementType());
                case CLASS:
                    if (b.kind() == Type.Kind.VOID) {
                        return a.asClassType().name().equals(DotName.createSimple(Void.class));
                    } else if (b.kind() == Type.Kind.CLASS) {
                        return isClassSubtype(a.asClassType(), b.asClassType());
                    } else if (b.kind() == Type.Kind.PARAMETERIZED_TYPE) {
                        return isClassSubtype(a.asClassType(), ClassType.create(b.name()));
                    } else if (b.kind() == Type.Kind.TYPE_VARIABLE) {
                        Type firstBound = b.asTypeVariable().bounds().isEmpty()
                                ? ClassType.OBJECT_TYPE
                                : b.asTypeVariable().bounds().get(0);
                        return isSubtype(a, firstBound);
                    } else {
                        return false;
                    }
                case PARAMETERIZED_TYPE:
                    if (b.kind() == Type.Kind.CLASS) {
                        return isClassSubtype(ClassType.create(a.name()), b.asClassType());
                    } else if (b.kind() == Type.Kind.PARAMETERIZED_TYPE) {
                        return isClassSubtype(ClassType.create(a.name()), ClassType.create(b.name()));
                    } else if (b.kind() == Type.Kind.TYPE_VARIABLE) {
                        Type firstBound = b.asTypeVariable().bounds().isEmpty()
                                ? ClassType.OBJECT_TYPE
                                : b.asTypeVariable().bounds().get(0);
                        return isSubtype(a, firstBound);
                    } else {
                        return false;
                    }
                case TYPE_VARIABLE:
                    if (b.kind() == Type.Kind.CLASS || b.kind() == Type.Kind.PARAMETERIZED_TYPE) {
                        Type firstBound = a.asTypeVariable().bounds().isEmpty()
                                ? ClassType.OBJECT_TYPE
                                : a.asTypeVariable().bounds().get(0);
                        return isSubtype(firstBound, b);
                    } else {
                        return false;
                    }
                default:
                    throw new IllegalArgumentException("Cannot determine assignability between " + a + " and " + b);
            }
        }

        boolean isSupertype(Type a, Type b) {
            return isSubtype(b, a);
        }

        private boolean isClassSubtype(ClassType a, ClassType b) {
            return assignabilityCheck.isAssignableFrom(b, a);
        }
    }
}
