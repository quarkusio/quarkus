package io.quarkus.arc.processor;

import io.quarkus.arc.BeanManagerProvider;
import io.quarkus.arc.BeanMetadataProvider;
import io.quarkus.arc.EventProvider;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.InjectionPointProvider;
import io.quarkus.arc.InstanceProvider;
import io.quarkus.arc.ResourceProvider;
import io.quarkus.arc.processor.InjectionPointInfo.InjectionPointKind;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.FunctionCreator;
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

    INSTANCE(DotNames.INSTANCE, new Generator() {
        @Override
        void generate(ClassOutput classOutput, BeanDeployment beanDeployment, InjectionPointInfo injectionPoint,
                ClassCreator clazzCreator,
                MethodCreator constructor, String providerName, AnnotationLiteralProcessor annotationLiterals) {

            ResultHandle qualifiers = BeanGenerator.collectQualifiers(classOutput, clazzCreator, beanDeployment, constructor,
                    injectionPoint,
                    annotationLiterals);
            ResultHandle parameterizedType = Types.getTypeHandle(constructor, injectionPoint.getRequiredType());
            ResultHandle annotationsHandle = BeanGenerator.collectAnnotations(classOutput, clazzCreator, beanDeployment,
                    constructor,
                    injectionPoint, annotationLiterals);
            ResultHandle javaMemberHandle = BeanGenerator.getJavaMemberHandle(constructor, injectionPoint);
            ResultHandle instanceProvider = constructor.newInstance(
                    MethodDescriptor.ofConstructor(InstanceProvider.class, java.lang.reflect.Type.class, Set.class,
                            InjectableBean.class, Set.class, Member.class, int.class),
                    parameterizedType, qualifiers, constructor.getThis(), annotationsHandle, javaMemberHandle,
                    constructor.load(injectionPoint.getPosition()));
            FunctionCreator instanceProviderSuppler = constructor.createFunction(Supplier.class);
            instanceProviderSuppler.getBytecode().returnValue(instanceProvider);
            constructor.writeInstanceField(
                    FieldDescriptor.of(clazzCreator.getClassName(), providerName, Supplier.class.getName()),
                    constructor.getThis(), instanceProviderSuppler.getInstance());

        }
    }, BuiltinBean::isInstanceInjectionPoint),
    INJECTION_POINT(DotNames.INJECTION_POINT,
            new Generator() {
                @Override
                void generate(ClassOutput classOutput, BeanDeployment beanDeployment, InjectionPointInfo injectionPoint,
                        ClassCreator clazzCreator,
                        MethodCreator constructor, String providerName, AnnotationLiteralProcessor annotationLiterals) {
                    // this.injectionPointProvider1 = () -> new InjectionPointProvider();
                    ResultHandle injectionPointProvider = constructor.newInstance(
                            MethodDescriptor.ofConstructor(InjectionPointProvider.class));
                    FunctionCreator injectionPointProviderSupplier = constructor.createFunction(Supplier.class);
                    injectionPointProviderSupplier.getBytecode().returnValue(injectionPointProvider);
                    constructor.writeInstanceField(
                            FieldDescriptor.of(clazzCreator.getClassName(), providerName,
                                    Supplier.class.getName()),
                            constructor.getThis(),
                            injectionPointProviderSupplier.getInstance());
                }
            }),
    BEAN(DotNames.BEAN, new Generator() {
        @Override
        void generate(ClassOutput classOutput, BeanDeployment beanDeployment, InjectionPointInfo injectionPoint,
                ClassCreator clazzCreator,
                MethodCreator constructor, String providerName, AnnotationLiteralProcessor annotationLiterals) {
            // this.beanProviderSupplier1 = () -> new BeanMetadataProvider<>();
            ResultHandle beanProvider = constructor.newInstance(MethodDescriptor.ofConstructor(BeanMetadataProvider.class));
            FunctionCreator beanProviderSupplier = constructor.createFunction(Supplier.class);
            beanProviderSupplier.getBytecode().returnValue(beanProvider);
            constructor.writeInstanceField(
                    FieldDescriptor.of(clazzCreator.getClassName(), providerName,
                            Supplier.class.getName()),
                    constructor.getThis(),
                    beanProviderSupplier.getInstance());
        }
    }),
    BEAN_MANAGER(DotNames.BEAN_MANAGER, new Generator() {
        @Override
        void generate(ClassOutput classOutput, BeanDeployment beanDeployment, InjectionPointInfo injectionPoint,
                ClassCreator clazzCreator,
                MethodCreator constructor, String providerName, AnnotationLiteralProcessor annotationLiterals) {
            ResultHandle beanManagerProvider = constructor.newInstance(
                    MethodDescriptor.ofConstructor(BeanManagerProvider.class));
            FunctionCreator injectionPointProviderSupplier = constructor.createFunction(Supplier.class);
            injectionPointProviderSupplier.getBytecode().returnValue(beanManagerProvider);
            constructor.writeInstanceField(
                    FieldDescriptor.of(clazzCreator.getClassName(), providerName,
                            Supplier.class.getName()),
                    constructor.getThis(),
                    injectionPointProviderSupplier.getInstance());
        }
    }),
    EVENT(DotNames.EVENT, new Generator() {
        @Override
        void generate(ClassOutput classOutput, BeanDeployment beanDeployment, InjectionPointInfo injectionPoint,
                ClassCreator clazzCreator,
                MethodCreator constructor, String providerName, AnnotationLiteralProcessor annotationLiterals) {

            ResultHandle qualifiers = constructor.newInstance(MethodDescriptor.ofConstructor(HashSet.class));
            if (!injectionPoint.getRequiredQualifiers().isEmpty()) {
                // Set<Annotation> instanceProvider1Qualifiers = new HashSet<>()
                // instanceProvider1Qualifiers.add(javax.enterprise.inject.Default.Literal.INSTANCE)

                for (AnnotationInstance qualifierAnnotation : injectionPoint.getRequiredQualifiers()) {
                    BuiltinQualifier qualifier = BuiltinQualifier.of(qualifierAnnotation);
                    if (qualifier != null) {
                        constructor.invokeInterfaceMethod(MethodDescriptors.SET_ADD, qualifiers,
                                qualifier.getLiteralInstance(constructor));
                    } else {
                        // Create annotation literal first
                        ClassInfo qualifierClass = beanDeployment.getQualifier(qualifierAnnotation.name());
                        constructor.invokeInterfaceMethod(MethodDescriptors.SET_ADD, qualifiers,
                                annotationLiterals.process(constructor, classOutput,
                                        qualifierClass, qualifierAnnotation,
                                        Types.getPackageName(clazzCreator.getClassName())));
                    }
                }
            }
            ResultHandle parameterizedType = Types.getTypeHandle(constructor, injectionPoint.getRequiredType());
            ResultHandle eventProvider = constructor.newInstance(
                    MethodDescriptor.ofConstructor(EventProvider.class, java.lang.reflect.Type.class,
                            Set.class),
                    parameterizedType, qualifiers);
            FunctionCreator eventProviderSupplier = constructor.createFunction(Supplier.class);
            eventProviderSupplier.getBytecode().returnValue(eventProvider);
            constructor.writeInstanceField(
                    FieldDescriptor.of(clazzCreator.getClassName(), providerName,
                            Supplier.class.getName()),
                    constructor.getThis(), eventProviderSupplier.getInstance());
        }
    }),
    RESOURCE(DotNames.OBJECT, new Generator() {
        @Override
        void generate(ClassOutput classOutput, BeanDeployment beanDeployment, InjectionPointInfo injectionPoint,
                ClassCreator clazzCreator,
                MethodCreator constructor, String providerName, AnnotationLiteralProcessor annotationLiterals) {

            ResultHandle annotations = constructor.newInstance(MethodDescriptor.ofConstructor(HashSet.class));
            // For a resource field the required qualifiers contain all annotations declared on the field
            if (!injectionPoint.getRequiredQualifiers().isEmpty()) {
                for (AnnotationInstance annotation : injectionPoint.getRequiredQualifiers()) {
                    // Create annotation literal first
                    ClassInfo annotationClass = beanDeployment.getIndex().getClassByName(annotation.name());
                    constructor.invokeInterfaceMethod(MethodDescriptors.SET_ADD, annotations,
                            annotationLiterals.process(constructor, classOutput,
                                    annotationClass, annotation,
                                    Types.getPackageName(clazzCreator.getClassName())));
                }
            }
            ResultHandle parameterizedType = Types.getTypeHandle(constructor, injectionPoint.getRequiredType());
            ResultHandle resourceProvider = constructor.newInstance(
                    MethodDescriptor.ofConstructor(ResourceProvider.class, java.lang.reflect.Type.class,
                            Set.class),
                    parameterizedType, annotations);
            FunctionCreator resourceProviderSupplier = constructor.createFunction(Supplier.class);
            resourceProviderSupplier.getBytecode().returnValue(resourceProvider);
            constructor.writeInstanceField(
                    FieldDescriptor.of(clazzCreator.getClassName(), providerName,
                            Supplier.class.getName()),
                    constructor.getThis(), resourceProviderSupplier.getInstance());
        }
    }, ip -> ip.getKind() == InjectionPointKind.RESOURCE),
    EVENT_METADATA(DotNames.EVENT_METADATA, new Generator() {
        @Override
        void generate(ClassOutput classOutput, BeanDeployment beanDeployment, InjectionPointInfo injectionPoint,
                ClassCreator clazzCreator, MethodCreator constructor, String providerName,
                AnnotationLiteralProcessor annotationLiterals) {
            // No-op
        }
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

    abstract static class Generator {

        abstract void generate(ClassOutput classOutput, BeanDeployment beanDeployment, InjectionPointInfo injectionPoint,
                ClassCreator clazzCreator,
                MethodCreator constructor, String providerName, AnnotationLiteralProcessor annotationLiterals);

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
