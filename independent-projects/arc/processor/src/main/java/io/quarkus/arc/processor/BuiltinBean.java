package io.quarkus.arc.processor;

import io.quarkus.arc.BeanManagerProvider;
import io.quarkus.arc.BeanMetadataProvider;
import io.quarkus.arc.EventProvider;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.InjectableReferenceProvider;
import io.quarkus.arc.InjectionPointProvider;
import io.quarkus.arc.InstanceProvider;
import io.quarkus.arc.InterceptedBeanMetadataProvider;
import io.quarkus.arc.ResourceProvider;
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
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

/**
 *
 * @author Martin Kouba
 */
enum BuiltinBean {

    INSTANCE(DotNames.INSTANCE, ctx -> {
        ResultHandle qualifiers = BeanGenerator.collectQualifiers(ctx.classOutput, ctx.clazzCreator, ctx.beanDeployment,
                ctx.constructor,
                ctx.injectionPoint,
                ctx.annotationLiterals);
        ResultHandle parameterizedType = Types.getTypeHandle(ctx.constructor, ctx.injectionPoint.getRequiredType());
        ResultHandle annotationsHandle = BeanGenerator.collectAnnotations(ctx.classOutput, ctx.clazzCreator, ctx.beanDeployment,
                ctx.constructor,
                ctx.injectionPoint, ctx.annotationLiterals);
        ResultHandle javaMemberHandle = BeanGenerator.getJavaMemberHandle(ctx.constructor, ctx.injectionPoint);
        ResultHandle instanceProvider = ctx.constructor.newInstance(
                MethodDescriptor.ofConstructor(InstanceProvider.class, java.lang.reflect.Type.class, Set.class,
                        InjectableBean.class, Set.class, Member.class, int.class),
                parameterizedType, qualifiers, ctx.constructor.getThis(), annotationsHandle, javaMemberHandle,
                ctx.constructor.load(ctx.injectionPoint.getPosition()));
        ctx.constructor.writeInstanceField(
                FieldDescriptor.of(ctx.clazzCreator.getClassName(), ctx.providerName,
                        InjectableReferenceProvider.class.getName()),
                ctx.constructor.getThis(), instanceProvider);

    }, BuiltinBean::isInstanceInjectionPoint),
    INJECTION_POINT(DotNames.INJECTION_POINT, ctx -> {
        // this.injectionPointProvider1 = new InjectionPointProvider();
        ctx.constructor.writeInstanceField(
                FieldDescriptor.of(ctx.clazzCreator.getClassName(), ctx.providerName,
                        InjectableReferenceProvider.class.getName()),
                ctx.constructor.getThis(),
                ctx.constructor.newInstance(MethodDescriptor.ofConstructor(InjectionPointProvider.class)));
    }),
    BEAN(DotNames.BEAN, ctx -> {
        // this.beanProvider1 = new BeanMetadataProvider<>();
        if (ctx.targetInfo.kind() != InjectionTargetInfo.TargetKind.BEAN) {
            throw new IllegalStateException("Invalid injection target info: " + ctx.targetInfo);
        }
        ctx.constructor.writeInstanceField(
                FieldDescriptor.of(ctx.clazzCreator.getClassName(), ctx.providerName,
                        InjectableReferenceProvider.class.getName()),
                ctx.constructor.getThis(),
                ctx.constructor.newInstance(MethodDescriptor.ofConstructor(BeanMetadataProvider.class, String.class),
                        ctx.constructor.load(ctx.targetInfo.asBean().getIdentifier())));
    }, ip -> {
        return isCdiAndRawTypeMatches(ip, DotNames.BEAN) && ip.hasDefaultedQualifier();
    }),
    INTERCEPTED_BEAN(DotNames.BEAN, ctx -> {
        if (!(ctx.targetInfo instanceof InterceptorInfo)) {
            throw new IllegalStateException("Invalid injection target info: " + ctx.targetInfo);
        }
        ctx.constructor.writeInstanceField(
                FieldDescriptor.of(ctx.clazzCreator.getClassName(), ctx.providerName,
                        InjectableReferenceProvider.class.getName()),
                ctx.constructor.getThis(),
                ctx.constructor.newInstance(MethodDescriptor.ofConstructor(InterceptedBeanMetadataProvider.class)));
    }, ip -> {
        return isCdiAndRawTypeMatches(ip, DotNames.BEAN) && !ip.hasDefaultedQualifier()
                && ip.getRequiredQualifiers().size() == 1
                && ip.getRequiredQualifiers().iterator().next().name().equals(DotNames.INTERCEPTED);
    }),
    BEAN_MANAGER(DotNames.BEAN_MANAGER, ctx -> {
        ctx.constructor.writeInstanceField(
                FieldDescriptor.of(ctx.clazzCreator.getClassName(), ctx.providerName,
                        InjectableReferenceProvider.class.getName()),
                ctx.constructor.getThis(),
                ctx.constructor.newInstance(MethodDescriptor.ofConstructor(BeanManagerProvider.class)));
    }),
    EVENT(DotNames.EVENT, ctx -> {

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
        ResultHandle parameterizedType = Types.getTypeHandle(ctx.constructor, ctx.injectionPoint.getRequiredType());
        ResultHandle eventProvider = ctx.constructor.newInstance(
                MethodDescriptor.ofConstructor(EventProvider.class, java.lang.reflect.Type.class,
                        Set.class),
                parameterizedType, qualifiers);
        ctx.constructor.writeInstanceField(
                FieldDescriptor.of(ctx.clazzCreator.getClassName(), ctx.providerName,
                        InjectableReferenceProvider.class.getName()),
                ctx.constructor.getThis(), eventProvider);
    }),
    RESOURCE(DotNames.OBJECT, ctx -> {

        ResultHandle annotations = ctx.constructor.newInstance(MethodDescriptor.ofConstructor(HashSet.class));
        // For a resource field the required qualifiers contain all annotations declared on the field
        if (!ctx.injectionPoint.getRequiredQualifiers().isEmpty()) {
            for (AnnotationInstance annotation : ctx.injectionPoint.getRequiredQualifiers()) {
                // Create annotation literal first
                ClassInfo annotationClass = ctx.beanDeployment.getIndex().getClassByName(annotation.name());
                ctx.constructor.invokeInterfaceMethod(MethodDescriptors.SET_ADD, annotations,
                        ctx.annotationLiterals.process(ctx.constructor, ctx.classOutput,
                                annotationClass, annotation,
                                Types.getPackageName(ctx.clazzCreator.getClassName())));
            }
        }
        ResultHandle parameterizedType = Types.getTypeHandle(ctx.constructor, ctx.injectionPoint.getRequiredType());
        ResultHandle resourceProvider = ctx.constructor.newInstance(
                MethodDescriptor.ofConstructor(ResourceProvider.class, java.lang.reflect.Type.class,
                        Set.class),
                parameterizedType, annotations);
        ctx.constructor.writeInstanceField(
                FieldDescriptor.of(ctx.clazzCreator.getClassName(), ctx.providerName,
                        InjectableReferenceProvider.class.getName()),
                ctx.constructor.getThis(), resourceProvider);
    }, ip -> ip.getKind() == InjectionPointKind.RESOURCE),
    EVENT_METADATA(DotNames.EVENT_METADATA, ctx -> {
    }),
    ;

    private final DotName rawTypeDotName;

    private final Generator generator;

    private final Predicate<InjectionPointInfo> matcher;

    BuiltinBean(DotName rawTypeDotName, Generator generator) {
        this(rawTypeDotName, generator, ip -> isCdiAndRawTypeMatches(ip, rawTypeDotName));
    }

    BuiltinBean(DotName rawTypeDotName, Generator generator, Predicate<InjectionPointInfo> matcher) {
        this.rawTypeDotName = rawTypeDotName;
        this.generator = generator;
        this.matcher = matcher;
    }

    boolean matches(InjectionPointInfo injectionPoint) {
        return matcher.test(injectionPoint);
    }

    DotName getRawTypeDotName() {
        return rawTypeDotName;
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

        public GeneratorContext(ClassOutput classOutput, BeanDeployment beanDeployment, InjectionPointInfo injectionPoint,
                ClassCreator clazzCreator, MethodCreator constructor, String providerName,
                AnnotationLiteralProcessor annotationLiterals, InjectionTargetInfo targetInfo) {
            this.classOutput = classOutput;
            this.beanDeployment = beanDeployment;
            this.injectionPoint = injectionPoint;
            this.clazzCreator = clazzCreator;
            this.constructor = constructor;
            this.providerName = providerName;
            this.annotationLiterals = annotationLiterals;
            this.targetInfo = targetInfo;
        }
    }

    @FunctionalInterface
    interface Generator {

        void generate(GeneratorContext context);

    }

    private static boolean isCdiAndRawTypeMatches(InjectionPointInfo injectionPoint, DotName rawTypeDotName) {
        if (injectionPoint.getKind() != InjectionPointKind.CDI) {
            return false;
        }
        return rawTypeDotName.equals(injectionPoint.getRequiredType().name());
    }

    private static boolean isInstanceInjectionPoint(InjectionPointInfo injectionPoint) {
        if (injectionPoint.getKind() != InjectionPointKind.CDI) {
            return false;
        }
        return DotNames.INSTANCE.equals(injectionPoint.getRequiredType().name())
                || DotNames.PROVIDER.equals(injectionPoint.getRequiredType().name());
    }

}
