package io.quarkus.arc.processor;

import static io.quarkus.arc.processor.IndexClassLookupUtils.getClassByName;
import static io.quarkus.arc.processor.KotlinUtils.isKotlinClass;

import java.lang.constant.ClassDesc;
import java.lang.reflect.Member;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.enterprise.inject.spi.DefinitionException;
import jakarta.enterprise.inject.spi.InjectionPoint;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Type;
import org.jboss.jandex.Type.Kind;

import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.WithCaching;
import io.quarkus.arc.impl.BeanManagerProvider;
import io.quarkus.arc.impl.BeanMetadataProvider;
import io.quarkus.arc.impl.EventProvider;
import io.quarkus.arc.impl.InjectionPointProvider;
import io.quarkus.arc.impl.InstanceProvider;
import io.quarkus.arc.impl.InterceptedDecoratedBeanMetadataProvider;
import io.quarkus.arc.impl.ListProvider;
import io.quarkus.arc.impl.ResourceProvider;
import io.quarkus.arc.processor.InjectionPointInfo.InjectionPointKind;
import io.quarkus.arc.processor.InjectionTargetInfo.TargetKind;
import io.quarkus.gizmo2.Const;
import io.quarkus.gizmo2.Expr;
import io.quarkus.gizmo2.LocalVar;
import io.quarkus.gizmo2.ParamVar;
import io.quarkus.gizmo2.Var;
import io.quarkus.gizmo2.creator.BlockCreator;
import io.quarkus.gizmo2.creator.ClassCreator;
import io.quarkus.gizmo2.desc.ConstructorDesc;
import io.quarkus.gizmo2.desc.FieldDesc;

/**
 *
 * @author Martin Kouba
 */
public enum BuiltinBean {

    INSTANCE(BuiltinBean::generateInstanceBytecode, BuiltinBean::cdiAndRawTypeMatches,
            BuiltinBean::validateInstance, DotNames.INSTANCE, DotNames.PROVIDER, DotNames.INJECTABLE_INSTANCE),
    INJECTION_POINT(BuiltinBean::generateInjectionPointBytecode,
            BuiltinBean::cdiAndRawTypeMatches, BuiltinBean::validateInjectionPoint, DotNames.INJECTION_POINT),
    BEAN(BuiltinBean::generateBeanBytecode,
            (ip, names) -> cdiAndRawTypeMatches(ip, DotNames.BEAN, DotNames.INJECTABLE_BEAN) && ip.hasDefaultedQualifier(),
            BuiltinBean::validateBean, DotNames.BEAN),
    INTERCEPTED_BEAN(BuiltinBean::generateInterceptedDecoratedBeanBytecode,
            (ip, names) -> cdiAndRawTypeMatches(ip, DotNames.BEAN, DotNames.INJECTABLE_BEAN) && !ip.hasDefaultedQualifier()
                    && ip.getRequiredQualifiers().size() == 1
                    && ip.getRequiredQualifiers().iterator().next().name().equals(DotNames.INTERCEPTED),
            BuiltinBean::validateInterceptedBean, DotNames.BEAN),
    DECORATED_BEAN(BuiltinBean::generateInterceptedDecoratedBeanBytecode,
            (ip, names) -> cdiAndRawTypeMatches(ip, DotNames.BEAN, DotNames.INJECTABLE_BEAN) && !ip.hasDefaultedQualifier()
                    && ip.getRequiredQualifiers().size() == 1
                    && ip.getRequiredQualifiers().iterator().next().name().equals(DotNames.DECORATED),
            BuiltinBean::validateDecoratedBean, DotNames.BEAN),
    BEAN_MANAGER(BuiltinBean::generateBeanManagerBytecode,
            DotNames.BEAN_MANAGER, DotNames.BEAN_CONTAINER),
    EVENT(BuiltinBean::generateEventBytecode, DotNames.EVENT),
    RESOURCE(BuiltinBean::generateResourceBytecode,
            (ip, names) -> ip.getKind() == InjectionPointKind.RESOURCE,
            DotNames.OBJECT),
    EVENT_METADATA(Generator.NOOP, BuiltinBean::cdiAndRawTypeMatches,
            BuiltinBean::validateEventMetadata, DotNames.EVENT_METADATA),
    LIST(BuiltinBean::generateListBytecode,
            (ip, names) -> cdiAndRawTypeMatches(ip, DotNames.LIST) && ip.getRequiredQualifier(DotNames.ALL) != null,
            BuiltinBean::validateList, DotNames.LIST),
    INTERCEPTION_PROXY(BuiltinBean::generateInterceptionProxyBytecode,
            BuiltinBean::cdiAndRawTypeMatches, BuiltinBean::validateInterceptionProxy, DotNames.INTERCEPTION_PROXY),
            ;

    private final DotName[] rawTypeDotNames;
    private final Generator generator;
    private final BiPredicate<InjectionPointInfo, DotName[]> matcher;
    private final Validator validator;

    BuiltinBean(Generator generator, DotName... rawTypeDotNames) {
        this(generator, BuiltinBean::cdiAndRawTypeMatches, rawTypeDotNames);
    }

    BuiltinBean(Generator generator, BiPredicate<InjectionPointInfo, DotName[]> matcher,
            DotName... rawTypeDotNames) {
        this(generator, matcher, Validator.NOOP, rawTypeDotNames);
    }

    BuiltinBean(Generator generator, BiPredicate<InjectionPointInfo, DotName[]> matcher,
            Validator validator, DotName... rawTypeDotNames) {
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
            BeanDeployment beanDeployment,
            InjectionTargetInfo injectionTarget,
            InjectionPointInfo injectionPoint,
            ClassCreator clazzCreator,
            BlockCreator constructor,
            FieldDesc providerField,
            AnnotationLiteralProcessor annotationLiterals,
            ReflectionRegistration reflectionRegistration,
            Predicate<DotName> injectionPointAnnotationsPredicate,
            // only applies to observers, `null` otherwise
            ParamVar declaringBeanSupplier) {
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
        Var qualifiers = BeanGenerator.collectInjectionPointQualifiers(
                ctx.beanDeployment, ctx.constructor, ctx.injectionPoint, ctx.annotationLiterals);
        LocalVar parameterizedType = RuntimeTypeCreator.of(ctx.constructor).create(ctx.injectionPoint.getType());

        // Note that we only collect the injection point metadata if needed, i.e. if any of the resolved beans is dependent,
        // and requires InjectionPoint metadata
        Set<BeanInfo> beans = ctx.beanDeployment.beanResolver.resolveBeans(ctx.injectionPoint.getRequiredType(),
                ctx.injectionPoint.getRequiredQualifiers());
        boolean collectMetadata = beans.stream()
                .anyMatch(b -> BuiltinScope.DEPENDENT.isDeclaredBy(b) && b.requiresInjectionPointMetadata());

        Expr bean;
        Expr annotations;
        Expr javaMember;
        if (collectMetadata) {
            bean = switch (ctx.injectionTarget.kind()) {
                case OBSERVER -> ctx.constructor.invokeInterface(MethodDescs.SUPPLIER_GET, ctx.declaringBeanSupplier);
                case BEAN -> ctx.clazzCreator.this_();
                case INVOKER -> loadInvokerTargetBean(ctx.injectionTarget.asInvoker(), ctx.constructor);
                default -> throw new IllegalStateException("Unsupported target info: " + ctx.injectionTarget);
            };
            annotations = BeanGenerator.collectInjectionPointAnnotations(
                    ctx.beanDeployment, ctx.constructor, ctx.injectionPoint, ctx.annotationLiterals,
                    ctx.injectionPointAnnotationsPredicate);
            javaMember = BeanGenerator.getJavaMember(ctx.constructor, ctx.injectionPoint,
                    ctx.reflectionRegistration);
        } else {
            bean = Const.ofNull(InjectableBean.class);
            annotations = collectWithCaching(ctx.beanDeployment, ctx.constructor, ctx.injectionPoint);
            javaMember = Const.ofNull(Member.class);
        }

        Expr instanceProvider = ctx.constructor.new_(ConstructorDesc.of(InstanceProvider.class,
                java.lang.reflect.Type.class, Set.class, InjectableBean.class, Set.class, Member.class,
                int.class, boolean.class),
                parameterizedType, qualifiers, bean, annotations, javaMember,
                Const.of(ctx.injectionPoint.getPosition()), Const.of(ctx.injectionPoint.isTransient()));
        Expr instanceProviderSupplier = ctx.constructor.new_(MethodDescs.FIXED_VALUE_SUPPLIER_CONSTRUCTOR, instanceProvider);
        ctx.constructor.set(ctx.clazzCreator.this_().field(ctx.providerField), instanceProviderSupplier);
    }

    private static void generateEventBytecode(GeneratorContext ctx) {
        LocalVar qualifiers = ctx.constructor.localVar("qualifiers", ctx.constructor.new_(HashSet.class));
        if (!ctx.injectionPoint.getRequiredQualifiers().isEmpty()) {
            // Set<Annotation> instanceProvider1Qualifiers = new HashSet<>()
            // instanceProvider1Qualifiers.add(jakarta.enterprise.inject.Default.Literal.INSTANCE)

            for (AnnotationInstance qualifier : ctx.injectionPoint.getRequiredQualifiers()) {
                BuiltinQualifier builtinQualifier = BuiltinQualifier.of(qualifier);
                if (builtinQualifier != null) {
                    ctx.constructor.withSet(qualifiers).add(builtinQualifier.getLiteralInstance());
                } else {
                    // Create annotation literal first
                    ClassInfo qualifierClass = ctx.beanDeployment.getQualifier(qualifier.name());
                    ctx.constructor.withSet(qualifiers).add(
                            ctx.annotationLiterals.create(ctx.constructor, qualifierClass, qualifier));
                }
            }
        }
        LocalVar parameterizedType = RuntimeTypeCreator.of(ctx.constructor).create(ctx.injectionPoint.getType());
        Var annotations = BeanGenerator.collectInjectionPointAnnotations(ctx.beanDeployment, ctx.constructor,
                ctx.injectionPoint, ctx.annotationLiterals, ctx.injectionPointAnnotationsPredicate);
        Var javaMember = BeanGenerator.getJavaMember(ctx.constructor, ctx.injectionPoint,
                ctx.reflectionRegistration);
        Expr bean = switch (ctx.injectionTarget.kind()) {
            case OBSERVER -> ctx.constructor.invokeInterface(MethodDescs.SUPPLIER_GET, ctx.declaringBeanSupplier);
            case BEAN -> ctx.clazzCreator.this_();
            case INVOKER -> loadInvokerTargetBean(ctx.injectionTarget.asInvoker(), ctx.constructor);
            default -> throw new IllegalStateException("Unsupported target info: " + ctx.injectionTarget);
        };

        Expr injectionPoint = ctx.constructor.new_(MethodDescs.INJECTION_POINT_IMPL_CONSTRUCTOR,
                parameterizedType, parameterizedType, qualifiers, bean, annotations, javaMember,
                Const.of(ctx.injectionPoint.getPosition()), Const.of(ctx.injectionPoint.isTransient()));

        Expr eventProvider = ctx.constructor.new_(ConstructorDesc.of(EventProvider.class,
                java.lang.reflect.Type.class, Set.class, InjectionPoint.class),
                parameterizedType, qualifiers, injectionPoint);
        Expr eventProviderSupplier = ctx.constructor.new_(MethodDescs.FIXED_VALUE_SUPPLIER_CONSTRUCTOR, eventProvider);
        ctx.constructor.set(ctx.clazzCreator.this_().field(ctx.providerField), eventProviderSupplier);
    }

    private static void generateInjectionPointBytecode(GeneratorContext ctx) {
        // this.injectionPointProvider1 = () -> new InjectionPointProvider();
        Expr injectionPointProvider = ctx.constructor.new_(InjectionPointProvider.class);
        Expr injectionPointProviderSupplier = ctx.constructor.new_(MethodDescs.FIXED_VALUE_SUPPLIER_CONSTRUCTOR,
                injectionPointProvider);
        ctx.constructor.set(ctx.clazzCreator.this_().field(ctx.providerField), injectionPointProviderSupplier);
    }

    private static void generateBeanBytecode(GeneratorContext ctx) {
        // this.beanProvider1 = () -> new BeanMetadataProvider<>();
        Expr beanProvider = ctx.constructor.new_(ConstructorDesc.of(BeanMetadataProvider.class, String.class),
                Const.of(ctx.injectionTarget.asBean().getIdentifier()));
        Expr beanProviderSupplier = ctx.constructor.new_(MethodDescs.FIXED_VALUE_SUPPLIER_CONSTRUCTOR, beanProvider);
        ctx.constructor.set(ctx.clazzCreator.this_().field(ctx.providerField), beanProviderSupplier);
    }

    private static void generateInterceptedDecoratedBeanBytecode(GeneratorContext ctx) {
        Expr beanMetadataProvider = ctx.constructor.new_(InterceptedDecoratedBeanMetadataProvider.class);
        Expr beanMetadataProviderSupplier = ctx.constructor.new_(
                MethodDescs.FIXED_VALUE_SUPPLIER_CONSTRUCTOR, beanMetadataProvider);
        ctx.constructor.set(ctx.clazzCreator.this_().field(ctx.providerField), beanMetadataProviderSupplier);
    }

    private static void generateBeanManagerBytecode(GeneratorContext ctx) {
        Expr beanManagerProvider = ctx.constructor.new_(BeanManagerProvider.class);
        Expr beanManagerProviderSupplier = ctx.constructor.new_(MethodDescs.FIXED_VALUE_SUPPLIER_CONSTRUCTOR,
                beanManagerProvider);
        ctx.constructor.set(ctx.clazzCreator.this_().field(ctx.providerField), beanManagerProviderSupplier);
    }

    private static void generateResourceBytecode(GeneratorContext ctx) {
        LocalVar annotations = ctx.constructor.localVar("annotations", ctx.constructor.new_(HashSet.class));
        // For a resource field the required qualifiers contain all runtime-retained annotations
        // declared on the field (hence we need to check if their classes are available)
        if (!ctx.injectionPoint.getRequiredQualifiers().isEmpty()) {
            for (AnnotationInstance annotation : ctx.injectionPoint.getRequiredQualifiers()) {
                ClassInfo annotationClass = getClassByName(ctx.beanDeployment.getBeanArchiveIndex(), annotation.name());
                if (annotationClass == null) {
                    continue;
                }
                ctx.constructor.withSet(annotations).add(
                        ctx.annotationLiterals.create(ctx.constructor, annotationClass, annotation));
            }
        }
        LocalVar parameterizedType = RuntimeTypeCreator.of(ctx.constructor).create(ctx.injectionPoint.getType());
        Expr resourceProvider = ctx.constructor.new_(ConstructorDesc.of(ResourceProvider.class,
                java.lang.reflect.Type.class, Set.class), parameterizedType, annotations);
        Expr resourceProviderSupplier = ctx.constructor.new_(MethodDescs.FIXED_VALUE_SUPPLIER_CONSTRUCTOR, resourceProvider);
        ctx.constructor.set(ctx.clazzCreator.this_().field(ctx.providerField), resourceProviderSupplier);
    }

    private static void generateListBytecode(GeneratorContext ctx) {
        // Register injection point for reflection
        InjectionPointInfo injectionPoint = ctx.injectionPoint;
        if (injectionPoint.isField()) {
            ctx.reflectionRegistration.registerField(injectionPoint.getAnnotationTarget().asField());
        } else if (injectionPoint.isParam()) {
            ctx.reflectionRegistration.registerMethod(injectionPoint.getAnnotationTarget().asMethodParameter().method());
        }

        LocalVar injectionPointType = RuntimeTypeCreator.of(ctx.constructor).create(ctx.injectionPoint.getType());

        // List<T> or List<InstanceHandle<T>
        LocalVar requiredType;
        Const usesInstance;
        Type type = ctx.injectionPoint.getType().asParameterizedType().arguments().get(0);
        if (type.name().equals(DotNames.INSTANCE_HANDLE)) {
            requiredType = RuntimeTypeCreator.of(ctx.constructor).create(type.asParameterizedType().arguments().get(0));
            usesInstance = Const.of(true);
        } else {
            requiredType = RuntimeTypeCreator.of(ctx.constructor).create(type);
            usesInstance = Const.of(false);
        }
        Var qualifiers = BeanGenerator.collectInjectionPointQualifiers(ctx.beanDeployment, ctx.constructor,
                ctx.injectionPoint, ctx.annotationLiterals);

        // Note that we only collect the injection point metadata if needed, i.e. if any of the resolved beans is dependent,
        // and requires InjectionPoint metadata
        Set<BeanInfo> beans = ctx.beanDeployment.beanResolver.resolveBeans(
                type.name().equals(DotNames.INSTANCE_HANDLE) ? type.asParameterizedType().arguments().get(0) : type,
                ctx.injectionPoint.getRequiredQualifiers()
                        .stream()
                        .filter(a -> !a.name().equals(DotNames.ALL))
                        .collect(Collectors.toSet()));
        boolean collectMetadata = beans.stream()
                .anyMatch(b -> BuiltinScope.DEPENDENT.isDeclaredBy(b) && b.requiresInjectionPointMetadata());

        Expr bean;
        Expr annotations;
        Expr javaMember;
        if (collectMetadata) {
            bean = switch (ctx.injectionTarget.kind()) {
                case OBSERVER -> ctx.constructor.invokeInterface(MethodDescs.SUPPLIER_GET, ctx.declaringBeanSupplier);
                case BEAN -> ctx.clazzCreator.this_();
                case INVOKER -> loadInvokerTargetBean(ctx.injectionTarget.asInvoker(), ctx.constructor);
                default -> throw new IllegalStateException("Unsupported target info: " + ctx.injectionTarget);
            };
            annotations = BeanGenerator.collectInjectionPointAnnotations(ctx.beanDeployment, ctx.constructor,
                    ctx.injectionPoint, ctx.annotationLiterals, ctx.injectionPointAnnotationsPredicate);
            javaMember = BeanGenerator.getJavaMember(ctx.constructor, ctx.injectionPoint,
                    ctx.reflectionRegistration);
        } else {
            bean = Const.ofNull(InjectableBean.class);
            annotations = Const.ofNull(Set.class);
            javaMember = Const.ofNull(Member.class);
        }

        Expr listProvider = ctx.constructor.new_(ConstructorDesc.of(ListProvider.class, java.lang.reflect.Type.class,
                java.lang.reflect.Type.class, Set.class, InjectableBean.class, Set.class, Member.class, int.class,
                boolean.class, boolean.class), requiredType, injectionPointType, qualifiers, bean, annotations,
                javaMember, Const.of(ctx.injectionPoint.getPosition()), Const.of(ctx.injectionPoint.isTransient()),
                usesInstance);
        Expr listProviderSupplier = ctx.constructor.new_(MethodDescs.FIXED_VALUE_SUPPLIER_CONSTRUCTOR, listProvider);
        ctx.constructor.set(ctx.clazzCreator.this_().field(ctx.providerField), listProviderSupplier);
    }

    private static void generateInterceptionProxyBytecode(GeneratorContext ctx) {
        BeanInfo bean = ctx.injectionTarget.asBean();
        String name = InterceptionProxyGenerator.interceptionProxyProviderName(bean);

        Expr supplier = ctx.constructor.new_(ConstructorDesc.of(ClassDesc.of(name)));
        ctx.constructor.set(ctx.clazzCreator.this_().field(ctx.providerField), supplier);
    }

    private static Expr loadInvokerTargetBean(InvokerInfo invoker, BlockCreator bc) {
        return bc.invokeInterface(MethodDescs.ARC_CONTAINER_BEAN, bc.invokeStatic(MethodDescs.ARC_REQUIRE_CONTAINER),
                Const.of(invoker.targetBean.getIdentifier()));
    }

    private static Expr collectWithCaching(BeanDeployment beanDeployment, BlockCreator bc,
            InjectionPointInfo injectionPoint) {

        AnnotationTarget annotationTarget = injectionPoint.isParam()
                ? injectionPoint.getAnnotationTarget().asMethodParameter().method()
                : injectionPoint.getAnnotationTarget();
        Expr annotations;
        if (!injectionPoint.isSynthetic()
                && Annotations.contains(beanDeployment.getAnnotations(annotationTarget), DotNames.WITH_CACHING)) {
            annotations = bc.setOf(Expr.staticField(FieldDesc.of(WithCaching.Literal.class, "INSTANCE")));
        } else {
            annotations = bc.setOf();
        }
        return annotations;
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

    private static void validateDecoratedBean(ValidatorContext ctx) {
        if (ctx.injectionTarget.kind() != InjectionTargetInfo.TargetKind.BEAN
                || !ctx.injectionTarget.asBean().isDecorator()) {
            ctx.errors.accept(new DefinitionException("Only decorators can access decorated bean metadata"));
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
