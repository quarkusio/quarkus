package io.quarkus.arc.processor;

import static io.quarkus.arc.processor.IndexClassLookupUtils.getClassByName;
import static io.quarkus.arc.processor.KotlinUtils.isKotlinClass;

import java.lang.reflect.Member;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import jakarta.enterprise.inject.spi.DefinitionException;
import jakarta.enterprise.inject.spi.InjectionPoint;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Type;
import org.jboss.jandex.Type.Kind;

import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.impl.BeanManagerProvider;
import io.quarkus.arc.impl.BeanMetadataProvider;
import io.quarkus.arc.impl.EventProvider;
import io.quarkus.arc.impl.InjectionPointProvider;
import io.quarkus.arc.impl.InstanceProvider;
import io.quarkus.arc.impl.InterceptedBeanMetadataProvider;
import io.quarkus.arc.impl.ListProvider;
import io.quarkus.arc.impl.ResourceProvider;
import io.quarkus.arc.processor.InjectionPointInfo.InjectionPointKind;
import io.quarkus.arc.processor.InjectionTargetInfo.TargetKind;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;

/**
 *
 * @author Martin Kouba
 */
public enum BuiltinBean {

    INSTANCE(BuiltinBean::generateInstanceBytecode, BuiltinBean::cdiAndRawTypeMatches,
            BuiltinBean::validateInstance, DotNames.INSTANCE, DotNames.PROVIDER, DotNames.INJECTABLE_INSTANCE),
    INJECTION_POINT(BuiltinBean::generateInjectionPointBytecode, BuiltinBean::cdiAndRawTypeMatches,
            BuiltinBean::validateInjectionPoint, DotNames.INJECTION_POINT),
    BEAN(BuiltinBean::generateBeanBytecode,
            (ip, names) -> cdiAndRawTypeMatches(ip, DotNames.BEAN, DotNames.INJECTABLE_BEAN) && ip.hasDefaultedQualifier(),
            BuiltinBean::validateBean, DotNames.BEAN),
    INTERCEPTED_BEAN(BuiltinBean::generateInterceptedBeanBytecode,
            (ip, names) -> cdiAndRawTypeMatches(ip, DotNames.BEAN, DotNames.INJECTABLE_BEAN) && !ip.hasDefaultedQualifier()
                    && ip.getRequiredQualifiers().size() == 1
                    && ip.getRequiredQualifiers().iterator().next().name().equals(DotNames.INTERCEPTED),
            BuiltinBean::validateInterceptedBean, DotNames.BEAN),
    BEAN_MANAGER(BuiltinBean::generateBeanManagerBytecode, DotNames.BEAN_MANAGER, DotNames.BEAN_CONTAINER),
    EVENT(BuiltinBean::generateEventBytecode, DotNames.EVENT),
    RESOURCE(BuiltinBean::generateResourceBytecode, (ip, names) -> ip.getKind() == InjectionPointKind.RESOURCE,
            DotNames.OBJECT),
    EVENT_METADATA(Generator.NOOP, BuiltinBean::cdiAndRawTypeMatches,
            BuiltinBean::validateEventMetadata, DotNames.EVENT_METADATA),
    LIST(BuiltinBean::generateListBytecode,
            (ip, names) -> cdiAndRawTypeMatches(ip, DotNames.LIST) && ip.getRequiredQualifier(DotNames.ALL) != null,
            BuiltinBean::validateList, DotNames.LIST),
    INTERCEPTION_PROXY(BuiltinBean::generateInterceptionProxyBytecode,
            BuiltinBean::cdiAndRawTypeMatches, BuiltinBean::validateInterceptionProxy,
            DotNames.INTERCEPTION_PROXY),
            ;

    private final DotName[] rawTypeDotNames;
    private final Generator generator;
    private final BiPredicate<InjectionPointInfo, DotName[]> matcher;
    private final Validator validator;

    private BuiltinBean(Generator generator, DotName... rawTypeDotNames) {
        this(generator, BuiltinBean::cdiAndRawTypeMatches, rawTypeDotNames);
    }

    private BuiltinBean(Generator generator, BiPredicate<InjectionPointInfo, DotName[]> matcher, DotName... rawTypeDotNames) {
        this(generator, matcher, Validator.NOOP, rawTypeDotNames);
    }

    private BuiltinBean(Generator generator, BiPredicate<InjectionPointInfo, DotName[]> matcher, Validator validator,
            DotName... rawTypeDotNames) {
        this.rawTypeDotNames = rawTypeDotNames;
        this.generator = generator;
        this.matcher = matcher;
        this.validator = validator;
    }

    public boolean matches(InjectionPointInfo injectionPoint) {
        return matcher.test(injectionPoint, rawTypeDotNames);
    }

    DotName[] getRawTypeDotNames() {
        return rawTypeDotNames;
    }

    boolean hasRawTypeDotName(DotName name) {
        for (DotName rawTypeDotName : rawTypeDotNames) {
            if (rawTypeDotName.equals(name)) {
                return true;
            }
        }
        return false;
    }

    Generator getGenerator() {
        return generator;
    }

    Validator getValidator() {
        return validator;
    }

    public static boolean resolvesTo(InjectionPointInfo injectionPoint) {
        return resolve(injectionPoint) != null;
    }

    public static BuiltinBean resolve(InjectionPointInfo injectionPoint) {
        for (BuiltinBean bean : values()) {
            if (bean.matches(injectionPoint)) {
                return bean;
            }
        }
        return null;
    }

    public record GeneratorContext(
            ClassOutput classOutput,
            BeanDeployment beanDeployment,
            InjectionPointInfo injectionPoint,
            ClassCreator clazzCreator,
            MethodCreator constructor,
            String providerName,
            AnnotationLiteralProcessor annotationLiterals,
            InjectionTargetInfo targetInfo,
            ReflectionRegistration reflectionRegistration,
            Predicate<DotName> injectionPointAnnotationsPredicate) {
    }

    @FunctionalInterface
    interface Generator {

        Generator NOOP = ctx -> {
        };

        void generate(GeneratorContext context);

    }

    public record ValidatorContext(
            BeanDeployment beanDeployment,
            InjectionTargetInfo injectionTarget,
            InjectionPointInfo injectionPoint,
            Consumer<Throwable> errors) {
    }

    @FunctionalInterface
    interface Validator {

        Validator NOOP = ctx -> {
        };

        void validate(ValidatorContext context);

    }

    private static boolean cdiAndRawTypeMatches(InjectionPointInfo injectionPoint, DotName... rawTypeDotNames) {
        if (injectionPoint.getKind() != InjectionPointKind.CDI) {
            return false;
        }
        for (DotName rawTypeDotName : rawTypeDotNames) {
            if (rawTypeDotName.equals(injectionPoint.getType().name())) {
                return true;
            }
        }
        return false;
    }

    private static void generateInstanceBytecode(GeneratorContext ctx) {
        ResultHandle qualifiers = BeanGenerator.collectInjectionPointQualifiers(
                ctx.beanDeployment,
                ctx.constructor, ctx.injectionPoint, ctx.annotationLiterals);
        ResultHandle parameterizedType = Types.getTypeHandle(ctx.constructor, ctx.injectionPoint.getType());
        ResultHandle annotationsHandle = BeanGenerator.collectInjectionPointAnnotations(
                ctx.beanDeployment,
                ctx.constructor, ctx.injectionPoint, ctx.annotationLiterals, ctx.injectionPointAnnotationsPredicate);
        ResultHandle javaMemberHandle = BeanGenerator.getJavaMemberHandle(ctx.constructor, ctx.injectionPoint,
                ctx.reflectionRegistration);
        ResultHandle beanHandle;
        switch (ctx.targetInfo.kind()) {
            case OBSERVER:
                // For observers the first argument is always the declaring bean
                beanHandle = ctx.constructor.invokeInterfaceMethod(
                        MethodDescriptors.SUPPLIER_GET, ctx.constructor.getMethodParam(0));
                break;
            case BEAN:
                beanHandle = ctx.constructor.getThis();
                break;
            case INVOKER:
                beanHandle = loadInvokerTargetBean(ctx.targetInfo.asInvoker(), ctx.constructor);
                break;
            default:
                throw new IllegalStateException("Unsupported target info: " + ctx.targetInfo);
        }
        ResultHandle instanceProvider = ctx.constructor.newInstance(
                MethodDescriptor.ofConstructor(InstanceProvider.class, java.lang.reflect.Type.class, Set.class,
                        InjectableBean.class, Set.class, Member.class, int.class, boolean.class),
                parameterizedType, qualifiers, beanHandle, annotationsHandle, javaMemberHandle,
                ctx.constructor.load(ctx.injectionPoint.getPosition()),
                ctx.constructor.load(ctx.injectionPoint.isTransient()));
        ResultHandle instanceProviderSupplier = ctx.constructor.newInstance(
                MethodDescriptors.FIXED_VALUE_SUPPLIER_CONSTRUCTOR, instanceProvider);
        ctx.constructor.writeInstanceField(
                FieldDescriptor.of(ctx.clazzCreator.getClassName(), ctx.providerName,
                        Supplier.class.getName()),
                ctx.constructor.getThis(), instanceProviderSupplier);
    }

    private static void generateEventBytecode(GeneratorContext ctx) {
        ResultHandle qualifiers = ctx.constructor.newInstance(MethodDescriptor.ofConstructor(HashSet.class));
        if (!ctx.injectionPoint.getRequiredQualifiers().isEmpty()) {
            // Set<Annotation> instanceProvider1Qualifiers = new HashSet<>()
            // instanceProvider1Qualifiers.add(jakarta.enterprise.inject.Default.Literal.INSTANCE)

            for (AnnotationInstance qualifierAnnotation : ctx.injectionPoint.getRequiredQualifiers()) {
                BuiltinQualifier qualifier = BuiltinQualifier.of(qualifierAnnotation);
                if (qualifier != null) {
                    ctx.constructor.invokeInterfaceMethod(MethodDescriptors.SET_ADD, qualifiers,
                            qualifier.getLiteralInstance(ctx.constructor));
                } else {
                    // Create annotation literal first
                    ClassInfo qualifierClass = ctx.beanDeployment.getQualifier(qualifierAnnotation.name());
                    ctx.constructor.invokeInterfaceMethod(MethodDescriptors.SET_ADD, qualifiers,
                            ctx.annotationLiterals.create(ctx.constructor, qualifierClass, qualifierAnnotation));
                }
            }
        }
        ResultHandle parameterizedType = Types.getTypeHandle(ctx.constructor, ctx.injectionPoint.getType());
        ResultHandle annotations = BeanGenerator.collectInjectionPointAnnotations(
                ctx.beanDeployment,
                ctx.constructor, ctx.injectionPoint, ctx.annotationLiterals, ctx.injectionPointAnnotationsPredicate);
        ResultHandle javaMember = BeanGenerator.getJavaMemberHandle(ctx.constructor, ctx.injectionPoint,
                ctx.reflectionRegistration);
        ResultHandle bean;
        switch (ctx.targetInfo.kind()) {
            case OBSERVER:
                // For observers the first argument is always the declaring bean
                bean = ctx.constructor.invokeInterfaceMethod(
                        MethodDescriptors.SUPPLIER_GET, ctx.constructor.getMethodParam(0));
                break;
            case BEAN:
                bean = ctx.constructor.getThis();
                break;
            case INVOKER:
                bean = loadInvokerTargetBean(ctx.targetInfo.asInvoker(), ctx.constructor);
                break;
            default:
                throw new IllegalStateException("Unsupported target info: " + ctx.targetInfo);
        }

        ResultHandle injectionPoint = ctx.constructor.newInstance(MethodDescriptors.INJECTION_POINT_IMPL_CONSTRUCTOR,
                parameterizedType, parameterizedType, qualifiers, bean, annotations, javaMember,
                ctx.constructor.load(ctx.injectionPoint.getPosition()),
                ctx.constructor.load(ctx.injectionPoint.isTransient()));

        ResultHandle eventProvider = ctx.constructor.newInstance(
                MethodDescriptor.ofConstructor(EventProvider.class, java.lang.reflect.Type.class,
                        Set.class, InjectionPoint.class),
                parameterizedType, qualifiers, injectionPoint);
        ResultHandle eventProviderSupplier = ctx.constructor.newInstance(
                MethodDescriptors.FIXED_VALUE_SUPPLIER_CONSTRUCTOR, eventProvider);
        ctx.constructor.writeInstanceField(
                FieldDescriptor.of(ctx.clazzCreator.getClassName(), ctx.providerName,
                        Supplier.class.getName()),
                ctx.constructor.getThis(), eventProviderSupplier);
    }

    private static void generateInjectionPointBytecode(GeneratorContext ctx) {
        // this.injectionPointProvider1 = () -> new InjectionPointProvider();
        ResultHandle injectionPointProvider = ctx.constructor.newInstance(
                MethodDescriptor.ofConstructor(InjectionPointProvider.class));
        ResultHandle injectionPointProviderSupplier = ctx.constructor.newInstance(
                MethodDescriptors.FIXED_VALUE_SUPPLIER_CONSTRUCTOR, injectionPointProvider);
        ctx.constructor.writeInstanceField(
                FieldDescriptor.of(ctx.clazzCreator.getClassName(), ctx.providerName,
                        Supplier.class.getName()),
                ctx.constructor.getThis(),
                injectionPointProviderSupplier);
    }

    private static void generateBeanBytecode(GeneratorContext ctx) {
        // this.beanProvider1 = () -> new BeanMetadataProvider<>();
        ResultHandle beanProvider = ctx.constructor.newInstance(
                MethodDescriptor.ofConstructor(BeanMetadataProvider.class, String.class),
                ctx.constructor.load(ctx.targetInfo.asBean().getIdentifier()));
        ResultHandle beanProviderSupplier = ctx.constructor.newInstance(
                MethodDescriptors.FIXED_VALUE_SUPPLIER_CONSTRUCTOR, beanProvider);
        ctx.constructor.writeInstanceField(
                FieldDescriptor.of(ctx.clazzCreator.getClassName(), ctx.providerName,
                        Supplier.class.getName()),
                ctx.constructor.getThis(),
                beanProviderSupplier);
    }

    private static void generateInterceptedBeanBytecode(GeneratorContext ctx) {
        ResultHandle interceptedBeanMetadataProvider = ctx.constructor
                .newInstance(MethodDescriptor.ofConstructor(InterceptedBeanMetadataProvider.class));

        ResultHandle interceptedBeanMetadataProviderSupplier = ctx.constructor.newInstance(
                MethodDescriptors.FIXED_VALUE_SUPPLIER_CONSTRUCTOR, interceptedBeanMetadataProvider);
        ctx.constructor.writeInstanceField(
                FieldDescriptor.of(ctx.clazzCreator.getClassName(), ctx.providerName,
                        Supplier.class.getName()),
                ctx.constructor.getThis(),
                interceptedBeanMetadataProviderSupplier);
    }

    private static void generateBeanManagerBytecode(GeneratorContext ctx) {
        ResultHandle beanManagerProvider = ctx.constructor.newInstance(
                MethodDescriptor.ofConstructor(BeanManagerProvider.class));
        ResultHandle injectionPointProviderSupplier = ctx.constructor.newInstance(
                MethodDescriptors.FIXED_VALUE_SUPPLIER_CONSTRUCTOR, beanManagerProvider);
        ctx.constructor.writeInstanceField(
                FieldDescriptor.of(ctx.clazzCreator.getClassName(), ctx.providerName,
                        Supplier.class.getName()),
                ctx.constructor.getThis(),
                injectionPointProviderSupplier);
    }

    private static void generateResourceBytecode(GeneratorContext ctx) {
        ResultHandle annotations = ctx.constructor.newInstance(MethodDescriptor.ofConstructor(HashSet.class));
        // For a resource field the required qualifiers contain all runtime-retained annotations
        // declared on the field (hence we need to check if their classes are available)
        if (!ctx.injectionPoint.getRequiredQualifiers().isEmpty()) {
            for (AnnotationInstance annotation : ctx.injectionPoint.getRequiredQualifiers()) {
                ClassInfo annotationClass = getClassByName(ctx.beanDeployment.getBeanArchiveIndex(), annotation.name());
                if (annotationClass == null) {
                    continue;
                }
                ctx.constructor.invokeInterfaceMethod(MethodDescriptors.SET_ADD, annotations,
                        ctx.annotationLiterals.create(ctx.constructor, annotationClass, annotation));
            }
        }
        ResultHandle parameterizedType = Types.getTypeHandle(ctx.constructor, ctx.injectionPoint.getType());
        ResultHandle resourceProvider = ctx.constructor.newInstance(
                MethodDescriptor.ofConstructor(ResourceProvider.class, java.lang.reflect.Type.class,
                        Set.class),
                parameterizedType, annotations);
        ResultHandle resourceProviderSupplier = ctx.constructor.newInstance(
                MethodDescriptors.FIXED_VALUE_SUPPLIER_CONSTRUCTOR, resourceProvider);
        ctx.constructor.writeInstanceField(
                FieldDescriptor.of(ctx.clazzCreator.getClassName(), ctx.providerName,
                        Supplier.class.getName()),
                ctx.constructor.getThis(), resourceProviderSupplier);
    }

    private static void generateListBytecode(GeneratorContext ctx) {
        // Register injection point for reflection
        InjectionPointInfo injectionPoint = ctx.injectionPoint;
        if (injectionPoint.isField()) {
            ctx.reflectionRegistration.registerField(injectionPoint.getAnnotationTarget().asField());
        } else if (injectionPoint.isParam()) {
            ctx.reflectionRegistration.registerMethod(injectionPoint.getAnnotationTarget().asMethodParameter().method());
        }

        MethodCreator mc = ctx.constructor;
        ResultHandle injectionPointType = Types.getTypeHandle(mc, ctx.injectionPoint.getType());

        // List<T> or List<InstanceHandle<T>
        ResultHandle requiredType;
        ResultHandle usesInstanceHandle;
        Type type = ctx.injectionPoint.getType().asParameterizedType().arguments().get(0);
        if (type.name().equals(DotNames.INSTANCE_HANDLE)) {
            requiredType = Types.getTypeHandle(mc, type.asParameterizedType().arguments().get(0));
            usesInstanceHandle = mc.load(true);
        } else {
            requiredType = Types.getTypeHandle(mc, type);
            usesInstanceHandle = mc.load(false);
        }

        ResultHandle qualifiers = BeanGenerator.collectInjectionPointQualifiers(
                ctx.beanDeployment,
                ctx.constructor, ctx.injectionPoint, ctx.annotationLiterals);
        ResultHandle annotationsHandle = BeanGenerator.collectInjectionPointAnnotations(
                ctx.beanDeployment,
                ctx.constructor, ctx.injectionPoint, ctx.annotationLiterals, ctx.injectionPointAnnotationsPredicate);
        ResultHandle javaMemberHandle = BeanGenerator.getJavaMemberHandle(ctx.constructor, ctx.injectionPoint,
                ctx.reflectionRegistration);
        ResultHandle beanHandle;
        switch (ctx.targetInfo.kind()) {
            case OBSERVER:
                // For observers the first argument is always the declaring bean
                beanHandle = ctx.constructor.invokeInterfaceMethod(
                        MethodDescriptors.SUPPLIER_GET, ctx.constructor.getMethodParam(0));
                break;
            case BEAN:
                beanHandle = ctx.constructor.getThis();
                break;
            case INVOKER:
                beanHandle = loadInvokerTargetBean(ctx.targetInfo.asInvoker(), ctx.constructor);
                break;
            default:
                throw new IllegalStateException("Unsupported target info: " + ctx.targetInfo);
        }
        ResultHandle listProvider = ctx.constructor.newInstance(
                MethodDescriptor.ofConstructor(ListProvider.class, java.lang.reflect.Type.class, java.lang.reflect.Type.class,
                        Set.class,
                        InjectableBean.class, Set.class, Member.class, int.class, boolean.class, boolean.class),
                requiredType, injectionPointType, qualifiers, beanHandle, annotationsHandle, javaMemberHandle,
                ctx.constructor.load(ctx.injectionPoint.getPosition()),
                ctx.constructor.load(ctx.injectionPoint.isTransient()), usesInstanceHandle);
        ResultHandle listProviderSupplier = ctx.constructor.newInstance(
                MethodDescriptors.FIXED_VALUE_SUPPLIER_CONSTRUCTOR, listProvider);
        ctx.constructor.writeInstanceField(
                FieldDescriptor.of(ctx.clazzCreator.getClassName(), ctx.providerName,
                        Supplier.class.getName()),
                ctx.constructor.getThis(), listProviderSupplier);
    }

    private static void generateInterceptionProxyBytecode(GeneratorContext ctx) {
        BeanInfo bean = ctx.targetInfo.asBean();
        String name = InterceptionProxyGenerator.interceptionProxyProviderName(bean);

        ResultHandle supplier = ctx.constructor.newInstance(MethodDescriptor.ofConstructor(name));
        ctx.constructor.writeInstanceField(
                FieldDescriptor.of(ctx.clazzCreator.getClassName(), ctx.providerName, Supplier.class.getName()),
                ctx.constructor.getThis(), supplier);
    }

    private static ResultHandle loadInvokerTargetBean(InvokerInfo invoker, BytecodeCreator bytecode) {
        ResultHandle arc = bytecode.invokeStaticMethod(MethodDescriptors.ARC_CONTAINER);
        return bytecode.invokeInterfaceMethod(MethodDescriptors.ARC_CONTAINER_BEAN, arc,
                bytecode.load(invoker.targetBean.getIdentifier()));
    }

    private static void validateInstance(ValidatorContext ctx) {
        if (ctx.injectionPoint.getType().kind() != Kind.PARAMETERIZED_TYPE) {
            ctx.errors.accept(new DefinitionException(
                    "An injection point of raw type jakarta.enterprise.inject.Instance is defined: "
                            + ctx.injectionPoint.getTargetInfo()));
        } else if (ctx.injectionPoint.getRequiredType().kind() == Kind.WILDCARD_TYPE) {
            ctx.errors.accept(new DefinitionException(
                    "Wildcard is not a legal type argument for jakarta.enterprise.inject.Instance: "
                            + ctx.injectionPoint.getTargetInfo()));
        } else if (ctx.injectionPoint.getRequiredType().kind() == Kind.TYPE_VARIABLE) {
            ctx.errors.accept(new DefinitionException(
                    "Type variable is not a legal type argument for jakarta.enterprise.inject.Instance: "
                            + ctx.injectionPoint.getTargetInfo()));
        }
    }

    private static void validateList(ValidatorContext ctx) {
        if (ctx.injectionPoint.getType().kind() != Kind.PARAMETERIZED_TYPE) {
            ctx.errors.accept(new DefinitionException(
                    "An injection point of raw type is defined: " + ctx.injectionPoint.getTargetInfo()));
        } else {
            // Note that at this point we can be sure that the required type is List<>
            Type typeParam = ctx.injectionPoint.getType().asParameterizedType().arguments().get(0);
            if (typeParam.kind() == Type.Kind.WILDCARD_TYPE) {
                if (ctx.injectionPoint.isSynthetic()) {
                    ctx.errors.accept(new DefinitionException(
                            "Wildcard is not a legal type argument for a synthetic @All List<?> injection point used in: "
                                    + ctx.injectionTarget.toString()));
                    return;
                }
                ClassInfo declaringClass;
                if (ctx.injectionPoint.isField()) {
                    declaringClass = ctx.injectionPoint.getAnnotationTarget().asField().declaringClass();
                } else {
                    declaringClass = ctx.injectionPoint.getAnnotationTarget().asMethodParameter().method().declaringClass();
                }
                if (isKotlinClass(declaringClass)) {
                    ctx.errors.accept(new DefinitionException(
                            "kotlin.collections.List cannot be used together with the @All qualifier, please use MutableList or java.util.List instead: "
                                    + ctx.injectionPoint.getTargetInfo()));
                } else {
                    ctx.errors.accept(new DefinitionException(
                            "Wildcard is not a legal type argument for: " + ctx.injectionPoint.getTargetInfo()));
                }
            } else if (typeParam.kind() == Type.Kind.TYPE_VARIABLE) {
                ctx.errors.accept(new DefinitionException(
                        "Type variable is not a legal type argument for: " + ctx.injectionPoint.getTargetInfo()));
            }
        }
    }

    private static void validateInjectionPoint(ValidatorContext ctx) {
        if (ctx.injectionTarget.kind() != TargetKind.BEAN
                || !BuiltinScope.DEPENDENT.is(ctx.injectionTarget.asBean().getScope())) {
            String msg = ctx.injectionPoint.getTargetInfo();
            if (msg.isBlank()) {
                msg = ctx.injectionTarget.toString();
            }
            ctx.errors.accept(new DefinitionException(
                    "Only @Dependent beans can access metadata about an injection point: " + msg));
        }
    }

    private static void validateBean(ValidatorContext ctx) {
        if (ctx.injectionTarget.kind() != InjectionTargetInfo.TargetKind.BEAN) {
            ctx.errors.accept(new DefinitionException("Only beans can access bean metadata"));
        }
    }

    private static void validateInterceptedBean(ValidatorContext ctx) {
        if (ctx.injectionTarget.kind() != InjectionTargetInfo.TargetKind.BEAN
                || !ctx.injectionTarget.asBean().isInterceptor()) {
            ctx.errors.accept(new DefinitionException("Only interceptors can access intercepted bean metadata"));
        }
    }

    private static void validateEventMetadata(ValidatorContext ctx) {
        if (ctx.injectionTarget.kind() != TargetKind.OBSERVER) {
            ctx.errors.accept(new DefinitionException(
                    "EventMetadata can be only injected into an observer method: " + ctx.injectionPoint.getTargetInfo()));
        }
    }

    private static void validateInterceptionProxy(ValidatorContext ctx) {
        if (ctx.injectionTarget.kind() != TargetKind.BEAN
                || (!ctx.injectionTarget.asBean().isProducerMethod() && !ctx.injectionTarget.asBean().isSynthetic())
                || ctx.injectionTarget.asBean().getInterceptionProxy() == null) {
            ctx.errors.accept(new DefinitionException(
                    "InterceptionProxy can only be injected into a producer method or a synthetic bean"));
        }
        if (ctx.injectionPoint.getType().kind() != Kind.PARAMETERIZED_TYPE) {
            ctx.errors.accept(new DefinitionException("InterceptionProxy must be a parameterized type"));
        }
        Type interceptionProxyType = ctx.injectionPoint.getType().asParameterizedType().arguments().get(0);
        if (interceptionProxyType.kind() != Kind.CLASS && interceptionProxyType.kind() != Kind.PARAMETERIZED_TYPE) {
            ctx.errors.accept(new DefinitionException(
                    "Type argument of InterceptionProxy may only be a class or parameterized type"));
        }
        if (!ctx.injectionTarget.asBean().getProviderType().equals(interceptionProxyType)) {
            String msg = ctx.injectionTarget.asBean().isProducerMethod()
                    ? "Type argument of InterceptionProxy must be equal to the return type of the producer method"
                    : "Type argument of InterceptionProxy must be equal to the bean provider type";
            ctx.errors.accept(new DefinitionException(msg));
        }
        ClassInfo clazz = getClassByName(ctx.beanDeployment.getBeanArchiveIndex(), interceptionProxyType.name());
        if (clazz != null) {
            if (clazz.isRecord()) {
                ctx.errors.accept(new DefinitionException("Cannot build InterceptionProxy for a record"));
            }
            if (clazz.isSealed()) {
                ctx.errors.accept(new DefinitionException("Cannot build InterceptionProxy for a sealed type"));
            }
        }
    }

}
