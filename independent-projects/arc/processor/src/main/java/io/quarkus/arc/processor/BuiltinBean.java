package io.quarkus.arc.processor;

import static io.quarkus.arc.processor.IndexClassLookupUtils.getClassByName;

import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.impl.BeanManagerProvider;
import io.quarkus.arc.impl.BeanMetadataProvider;
import io.quarkus.arc.impl.EventProvider;
import io.quarkus.arc.impl.InjectionPointProvider;
import io.quarkus.arc.impl.InstanceProvider;
import io.quarkus.arc.impl.InterceptedBeanMetadataProvider;
import io.quarkus.arc.impl.ResourceProvider;
import io.quarkus.arc.processor.InjectionPointInfo.InjectionPointKind;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import java.lang.reflect.Member;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

/**
 *
 * @author Martin Kouba
 */
enum BuiltinBean {

    INSTANCE(ctx -> {
        ResultHandle qualifiers = BeanGenerator.collectInjectionPointQualifiers(ctx.classOutput, ctx.clazzCreator,
                ctx.beanDeployment,
                ctx.constructor, ctx.injectionPoint, ctx.annotationLiterals);
        ResultHandle parameterizedType = Types.getTypeHandle(ctx.constructor, ctx.injectionPoint.getType());
        ResultHandle annotationsHandle = BeanGenerator.collectInjectionPointAnnotations(ctx.classOutput, ctx.clazzCreator,
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
                        InjectableBean.class, Set.class, Member.class, int.class),
                parameterizedType, qualifiers, beanHandle, annotationsHandle, javaMemberHandle,
                ctx.constructor.load(ctx.injectionPoint.getPosition()));
        ResultHandle instanceProviderSupplier = ctx.constructor.newInstance(
                MethodDescriptors.FIXED_VALUE_SUPPLIER_CONSTRUCTOR, instanceProvider);
        ctx.constructor.writeInstanceField(
                FieldDescriptor.of(ctx.clazzCreator.getClassName(), ctx.providerName,
                        Supplier.class.getName()),
                ctx.constructor.getThis(), instanceProviderSupplier);
    }, DotNames.INSTANCE, DotNames.PROVIDER, DotNames.INJECTABLE_INSTANCE),
    INJECTION_POINT(ctx -> {
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
    }, DotNames.INJECTION_POINT),
    BEAN(ctx -> {
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
    }, ip -> {
        return isCdiAndRawTypeMatches(ip, DotNames.BEAN, DotNames.INJECTABLE_BEAN) && ip.hasDefaultedQualifier();
    }, DotNames.BEAN),
    INTERCEPTED_BEAN(ctx -> {
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
    }, ip -> {
        return isCdiAndRawTypeMatches(ip, DotNames.BEAN, DotNames.INJECTABLE_BEAN) && !ip.hasDefaultedQualifier()
                && ip.getRequiredQualifiers().size() == 1
                && ip.getRequiredQualifiers().iterator().next().name().equals(DotNames.INTERCEPTED);
    }, DotNames.BEAN),
    BEAN_MANAGER(ctx -> {
        ResultHandle beanManagerProvider = ctx.constructor.newInstance(
                MethodDescriptor.ofConstructor(BeanManagerProvider.class));
        ResultHandle injectionPointProviderSupplier = ctx.constructor.newInstance(
                MethodDescriptors.FIXED_VALUE_SUPPLIER_CONSTRUCTOR, beanManagerProvider);
        ctx.constructor.writeInstanceField(
                FieldDescriptor.of(ctx.clazzCreator.getClassName(), ctx.providerName,
                        Supplier.class.getName()),
                ctx.constructor.getThis(),
                injectionPointProviderSupplier);
    }, DotNames.BEAN_MANAGER),
    EVENT(ctx -> {

        ResultHandle qualifiers = ctx.constructor.newInstance(MethodDescriptor.ofConstructor(HashSet.class));
        if (!ctx.injectionPoint.getRequiredQualifiers().isEmpty()) {
            // Set<Annotation> instanceProvider1Qualifiers = new HashSet<>()
            // instanceProvider1Qualifiers.add(javax.enterprise.inject.Default.Literal.INSTANCE)

            for (AnnotationInstance qualifierAnnotation : ctx.injectionPoint.getRequiredQualifiers()) {
                BuiltinQualifier qualifier = BuiltinQualifier.of(qualifierAnnotation);
                if (qualifier != null) {
                    ctx.constructor.invokeInterfaceMethod(MethodDescriptors.SET_ADD, qualifiers,
                            qualifier.getLiteralInstance(ctx.constructor));
                } else {
                    // Create annotation literal first
                    ClassInfo qualifierClass = ctx.beanDeployment.getQualifier(qualifierAnnotation.name());
                    ctx.constructor.invokeInterfaceMethod(MethodDescriptors.SET_ADD, qualifiers,
                            ctx.annotationLiterals.process(ctx.constructor, ctx.classOutput,
                                    qualifierClass, qualifierAnnotation,
                                    Types.getPackageName(ctx.clazzCreator.getClassName())));
                }
            }
        }
        ResultHandle parameterizedType = Types.getTypeHandle(ctx.constructor, ctx.injectionPoint.getType());
        ResultHandle eventProvider = ctx.constructor.newInstance(
                MethodDescriptor.ofConstructor(EventProvider.class, java.lang.reflect.Type.class,
                        Set.class),
                parameterizedType, qualifiers);
        ResultHandle eventProviderSupplier = ctx.constructor.newInstance(
                MethodDescriptors.FIXED_VALUE_SUPPLIER_CONSTRUCTOR, eventProvider);
        ctx.constructor.writeInstanceField(
                FieldDescriptor.of(ctx.clazzCreator.getClassName(), ctx.providerName,
                        Supplier.class.getName()),
                ctx.constructor.getThis(), eventProviderSupplier);
    }, DotNames.EVENT),
    RESOURCE(ctx -> {

        ResultHandle annotations = ctx.constructor.newInstance(MethodDescriptor.ofConstructor(HashSet.class));
        // For a resource field the required qualifiers contain all annotations declared on the field
        if (!ctx.injectionPoint.getRequiredQualifiers().isEmpty()) {
            for (AnnotationInstance annotation : ctx.injectionPoint.getRequiredQualifiers()) {
                // Create annotation literal first
                ClassInfo annotationClass = getClassByName(ctx.beanDeployment.getBeanArchiveIndex(), annotation.name());
                ctx.constructor.invokeInterfaceMethod(MethodDescriptors.SET_ADD, annotations,
                        ctx.annotationLiterals.process(ctx.constructor, ctx.classOutput,
                                annotationClass, annotation,
                                Types.getPackageName(ctx.clazzCreator.getClassName())));
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
    }, ip -> ip.getKind() == InjectionPointKind.RESOURCE, DotNames.OBJECT),
    EVENT_METADATA(ctx -> {
    }, DotNames.EVENT_METADATA),
    ;

    private final DotName[] rawTypeDotNames;

    private final Generator generator;

    private final Predicate<InjectionPointInfo> matcher;

    BuiltinBean(Generator generator, DotName... rawTypeDotNames) {
        this(generator, ip -> isCdiAndRawTypeMatches(ip, rawTypeDotNames), rawTypeDotNames);
    }

    BuiltinBean(Generator generator, Predicate<InjectionPointInfo> matcher, DotName... rawTypeDotNames) {
        this.rawTypeDotNames = rawTypeDotNames;
        this.generator = generator;
        this.matcher = matcher;
    }

    boolean matches(InjectionPointInfo injectionPoint) {
        return matcher.test(injectionPoint);
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

        void generate(GeneratorContext context);

    }

    private static boolean isCdiAndRawTypeMatches(InjectionPointInfo injectionPoint, DotName... rawTypeDotNames) {
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

}
