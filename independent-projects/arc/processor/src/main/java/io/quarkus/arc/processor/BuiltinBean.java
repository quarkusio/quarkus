package io.quarkus.arc.processor;

import static io.quarkus.arc.processor.IndexClassLookupUtils.getClassByName;

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
enum BuiltinBean {

    INSTANCE(BuiltinBean::generateInstanceBytecode, BuiltinBean::cdiAndRawTypeMatches,
            BuiltinBean::validateInstance, DotNames.INSTANCE, DotNames.PROVIDER, DotNames.INJECTABLE_INSTANCE),
    INJECTION_POINT(BuiltinBean::generateInjectionPointBytecode, BuiltinBean::cdiAndRawTypeMatches,
            BuiltinBean::validateInjectionPoint, DotNames.INJECTION_POINT),
    BEAN(BuiltinBean::generateBeanBytecode,
            (ip, names) -> cdiAndRawTypeMatches(ip, DotNames.BEAN, DotNames.INJECTABLE_BEAN) && ip.hasDefaultedQualifier(),
            DotNames.BEAN),

    INTERCEPTED_BEAN(BuiltinBean::generateInterceptedBeanBytecode,
            (ip, names) -> cdiAndRawTypeMatches(ip, DotNames.BEAN, DotNames.INJECTABLE_BEAN) && !ip.hasDefaultedQualifier()
                    && ip.getRequiredQualifiers().size() == 1
                    && ip.getRequiredQualifiers().iterator().next().name().equals(DotNames.INTERCEPTED),
            DotNames.BEAN),
    BEAN_MANAGER(BuiltinBean::generateBeanManagerBytecode, DotNames.BEAN_MANAGER, DotNames.BEAN_CONTAINER),
    EVENT(BuiltinBean::generateEventBytecode, DotNames.EVENT),
    RESOURCE(BuiltinBean::generateResourceBytecode, (ip, names) -> ip.getKind() == InjectionPointKind.RESOURCE,
            DotNames.OBJECT),
    EVENT_METADATA(Generator.NOOP, BuiltinBean::cdiAndRawTypeMatches,
            BuiltinBean::validateEventMetadata, DotNames.EVENT_METADATA),
    LIST(BuiltinBean::generateListBytecode,
            (ip, names) -> cdiAndRawTypeMatches(ip, DotNames.LIST) && ip.getRequiredQualifier(DotNames.ALL) != null,
            DotNames.LIST),
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

    boolean matches(InjectionPointInfo injectionPoint) {
        return matcher.test(injectionPoint, rawTypeDotNames);
    }

    void validate(InjectionTargetInfo injectionTarget, InjectionPointInfo injectionPoint, Consumer<Throwable> errors) {
        validator.validate(injectionTarget, injectionPoint, errors);
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

    static boolean resolvesTo(InjectionPointInfo injectionPoint) {
        return resolve(injectionPoint) != null;
    }

    static BuiltinBean resolve(InjectionPointInfo injectionPoint) {
        for (BuiltinBean bean : values()) {
            if (bean.matches(injectionPoint)) {
                return bean;
            }
        }
        return null;
    }

    public static class GeneratorContext {

        final ClassOutput classOutput;
        final BeanDeployment beanDeployment;
        final InjectionPointInfo injectionPoint;
        final ClassCreator clazzCreator;
        final MethodCreator constructor;
        final String providerName;
        final AnnotationLiteralProcessor annotationLiterals;
        final InjectionTargetInfo targetInfo;
        final ReflectionRegistration reflectionRegistration;
        final Predicate<DotName> injectionPointAnnotationsPredicate;

        public GeneratorContext(ClassOutput classOutput, BeanDeployment beanDeployment, InjectionPointInfo injectionPoint,
                ClassCreator clazzCreator, MethodCreator constructor, String providerName,
                AnnotationLiteralProcessor annotationLiterals, InjectionTargetInfo targetInfo,
                ReflectionRegistration reflectionRegistration, Predicate<DotName> injectionPointAnnotationsPredicate) {
            this.classOutput = classOutput;
            this.beanDeployment = beanDeployment;
            this.injectionPoint = injectionPoint;
            this.clazzCreator = clazzCreator;
            this.constructor = constructor;
            this.providerName = providerName;
            this.annotationLiterals = annotationLiterals;
            this.targetInfo = targetInfo;
            this.reflectionRegistration = reflectionRegistration;
            this.injectionPointAnnotationsPredicate = injectionPointAnnotationsPredicate;
        }
    }

    @FunctionalInterface
    interface Generator {

        Generator NOOP = ctx -> {
        };

        void generate(GeneratorContext context);

    }

    @FunctionalInterface
    interface Validator {

        Validator NOOP = (it, ip, e) -> {
        };

        void validate(InjectionTargetInfo injectionTarget, InjectionPointInfo injectionPoint, Consumer<Throwable> errors);

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
        if (ctx.targetInfo.kind() != InjectionTargetInfo.TargetKind.BEAN) {
            throw new IllegalStateException("Invalid injection target info: " + ctx.targetInfo);
        }
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
        if (!(ctx.targetInfo instanceof InterceptorInfo)) {
            throw new IllegalStateException("Invalid injection target info: " + ctx.targetInfo);
        }
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
            ctx.reflectionRegistration.registerField(injectionPoint.getTarget().asField());
        } else {
            ctx.reflectionRegistration.registerMethod(injectionPoint.getTarget().asMethod());
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

    private static void validateInstance(InjectionTargetInfo injectionTarget, InjectionPointInfo injectionPoint,
            Consumer<Throwable> errors) {
        if (injectionPoint.getType().kind() != Kind.PARAMETERIZED_TYPE) {
            errors.accept(
                    new DefinitionException("An injection point of raw type jakarta.enterprise.inject.Instance is defined: "
                            + injectionPoint.getTargetInfo()));
        } else if (injectionPoint.getRequiredType().kind() == Kind.WILDCARD_TYPE) {
            errors.accept(
                    new DefinitionException("Wildcard is not a legal type argument for jakarta.enterprise.inject.Instance: " +
                            injectionPoint.getTargetInfo()));
        } else if (injectionPoint.getRequiredType().kind() == Kind.TYPE_VARIABLE) {
            errors.accept(new DefinitionException(
                    "Type variable is not a legal type argument for jakarta.enterprise.inject.Instance: " +
                            injectionPoint.getTargetInfo()));
        }
    }

    private static void validateInjectionPoint(InjectionTargetInfo injectionTarget, InjectionPointInfo injectionPoint,
            Consumer<Throwable> errors) {
        if (injectionTarget.kind() != TargetKind.BEAN || !BuiltinScope.DEPENDENT.is(injectionTarget.asBean().getScope())) {
            String msg = injectionPoint.getTargetInfo();
            if (msg.isBlank()) {
                msg = injectionTarget.toString();
            }
            errors.accept(new DefinitionException("Only @Dependent beans can access metadata about an injection point: "
                    + msg));
        }
    }

    private static void validateEventMetadata(InjectionTargetInfo injectionTarget, InjectionPointInfo injectionPoint,
            Consumer<Throwable> errors) {
        if (injectionTarget.kind() != TargetKind.OBSERVER) {
            errors.accept(new DefinitionException("EventMetadata can be only injected into an observer method: "
                    + injectionPoint.getTargetInfo()));
        }
    }

}
