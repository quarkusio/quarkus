package org.jboss.protean.arc.processor;

import java.util.HashSet;
import java.util.Set;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.protean.arc.BeanManagerProvider;
import org.jboss.protean.arc.BeanMetadataProvider;
import org.jboss.protean.arc.EventProvider;
import org.jboss.protean.arc.InjectableReferenceProvider;
import org.jboss.protean.arc.InjectionPointProvider;
import org.jboss.protean.arc.InstanceProvider;
import org.jboss.protean.gizmo.ClassCreator;
import org.jboss.protean.gizmo.ClassOutput;
import org.jboss.protean.gizmo.FieldDescriptor;
import org.jboss.protean.gizmo.MethodCreator;
import org.jboss.protean.gizmo.MethodDescriptor;
import org.jboss.protean.gizmo.ResultHandle;

/**
 *
 * @author Martin Kouba
 */
enum BuiltinBean {

    INSTANCE(DotNames.INSTANCE, new Generator() {
        void generate(ClassOutput classOutput, BeanDeployment beanDeployment, InjectionPointInfo injectionPoint, ClassCreator clazzCreator,
                MethodCreator constructor, String providerName, AnnotationLiteralProcessor annotationLiterals) {

            ResultHandle qualifiers = constructor.newInstance(MethodDescriptor.ofConstructor(HashSet.class));
            if (!injectionPoint.requiredQualifiers.isEmpty()) {
                // Set<Annotation> instanceProvider1Qualifiers = new HashSet<>()
                // instanceProvider1Qualifiers.add(javax.enterprise.inject.Default.Literal.INSTANCE)

                for (AnnotationInstance qualifierAnnotation : injectionPoint.requiredQualifiers) {
                    BuiltinQualifier qualifier = BuiltinQualifier.of(qualifierAnnotation);
                    if (qualifier != null) {
                        constructor.invokeInterfaceMethod(MethodDescriptors.SET_ADD, qualifiers, qualifier.getLiteralInstance(constructor));
                    } else {
                        // Create annotation literal first
                        ClassInfo qualifierClass = beanDeployment.getQualifier(qualifierAnnotation.name());
                        String annotationLiteralName = annotationLiterals.process(classOutput, qualifierClass, qualifierAnnotation,
                                Types.getPackageName(clazzCreator.getClassName()));
                        constructor.invokeInterfaceMethod(MethodDescriptors.SET_ADD, qualifiers,
                                constructor.newInstance(MethodDescriptor.ofConstructor(annotationLiteralName)));
                    }
                }
            }
            ResultHandle parameterizedType = Types.getTypeHandle(constructor, injectionPoint.requiredType);
            ResultHandle instanceProvider = constructor.newInstance(
                    MethodDescriptor.ofConstructor(InstanceProvider.class, java.lang.reflect.Type.class, Set.class), parameterizedType, qualifiers);
            constructor.writeInstanceField(FieldDescriptor.of(clazzCreator.getClassName(), providerName, InjectableReferenceProvider.class.getName()),
                    constructor.getThis(), instanceProvider);

        }
    }), INJECTION_POINT(DotNames.INJECTION_POINT, new Generator() {
        void generate(ClassOutput classOutput, BeanDeployment beanDeployment, InjectionPointInfo injectionPoint, ClassCreator clazzCreator,
                MethodCreator constructor, String providerName, AnnotationLiteralProcessor annotationLiterals) {
            // this.injectionPointProvider1 = new InjectionPointProvider();
            constructor.writeInstanceField(FieldDescriptor.of(clazzCreator.getClassName(), providerName, InjectableReferenceProvider.class.getName()),
                    constructor.getThis(), constructor.newInstance(MethodDescriptor.ofConstructor(InjectionPointProvider.class)));
        }
    }), BEAN(DotNames.BEAN, new Generator() {
        void generate(ClassOutput classOutput, BeanDeployment beanDeployment, InjectionPointInfo injectionPoint, ClassCreator clazzCreator,
                MethodCreator constructor, String providerName, AnnotationLiteralProcessor annotationLiterals) {
            // this.beanProvider1 = new BeanMetadataProvider<>();
            constructor.writeInstanceField(FieldDescriptor.of(clazzCreator.getClassName(), providerName, InjectableReferenceProvider.class.getName()),
                    constructor.getThis(), constructor.newInstance(MethodDescriptor.ofConstructor(BeanMetadataProvider.class)));
        }
    }), BEAN_MANAGER(DotNames.BEAN_MANAGER, new Generator() {
        void generate(ClassOutput classOutput, BeanDeployment beanDeployment, InjectionPointInfo injectionPoint, ClassCreator clazzCreator,
                MethodCreator constructor, String providerName, AnnotationLiteralProcessor annotationLiterals) {
            // TODO dummy provider
            constructor.writeInstanceField(FieldDescriptor.of(clazzCreator.getClassName(), providerName, InjectableReferenceProvider.class.getName()),
                    constructor.getThis(), constructor.newInstance(MethodDescriptor.ofConstructor(BeanManagerProvider.class)));
        }
    }), EVENT(DotNames.EVENT, new Generator() {
        @Override
        void generate(ClassOutput classOutput, BeanDeployment beanDeployment, InjectionPointInfo injectionPoint, ClassCreator clazzCreator,
                MethodCreator constructor, String providerName, AnnotationLiteralProcessor annotationLiterals) {

            ResultHandle qualifiers = constructor.newInstance(MethodDescriptor.ofConstructor(HashSet.class));
            if (!injectionPoint.requiredQualifiers.isEmpty()) {
                // Set<Annotation> instanceProvider1Qualifiers = new HashSet<>()
                // instanceProvider1Qualifiers.add(javax.enterprise.inject.Default.Literal.INSTANCE)

                for (AnnotationInstance qualifierAnnotation : injectionPoint.requiredQualifiers) {
                    BuiltinQualifier qualifier = BuiltinQualifier.of(qualifierAnnotation);
                    if (qualifier != null) {
                        constructor.invokeInterfaceMethod(MethodDescriptors.SET_ADD, qualifiers, qualifier.getLiteralInstance(constructor));
                    } else {
                        // Create annotation literal first
                        ClassInfo qualifierClass = beanDeployment.getQualifier(qualifierAnnotation.name());
                        String annotationLiteralName = annotationLiterals.process(classOutput, qualifierClass, qualifierAnnotation,
                                Types.getPackageName(clazzCreator.getClassName()));
                        constructor.invokeInterfaceMethod(MethodDescriptors.SET_ADD, qualifiers,
                                constructor.newInstance(MethodDescriptor.ofConstructor(annotationLiteralName)));
                    }
                }
            }
            ResultHandle parameterizedType = Types.getTypeHandle(constructor, injectionPoint.requiredType);
            ResultHandle eventProvider = constructor.newInstance(
                    MethodDescriptor.ofConstructor(EventProvider.class, java.lang.reflect.Type.class, Set.class), parameterizedType, qualifiers);
            constructor.writeInstanceField(FieldDescriptor.of(clazzCreator.getClassName(), providerName, InjectableReferenceProvider.class.getName()),
                    constructor.getThis(), eventProvider);
        }
    });

    private final DotName rawTypeDotName;

    private final Generator generator;

    BuiltinBean(DotName rawTypeDotName, Generator generator) {
        this.rawTypeDotName = rawTypeDotName;
        this.generator = generator;
    }

    public DotName getRawTypeDotName() {
        return rawTypeDotName;
    }

    public Generator getGenerator() {
        return generator;
    }

    static boolean resolvesTo(InjectionPointInfo injectionPoint) {
        return resolve(injectionPoint) != null;
    }

    static BuiltinBean resolve(InjectionPointInfo injectionPoint) {
        for (BuiltinBean bean : values()) {
            if (bean.getRawTypeDotName().equals(injectionPoint.requiredType.name())) {
                return bean;
            }
        }
        return null;
    }

    abstract static class Generator {

        abstract void generate(ClassOutput classOutput, BeanDeployment beanDeployment, InjectionPointInfo injectionPoint, ClassCreator clazzCreator,
                MethodCreator constructor, String providerName, AnnotationLiteralProcessor annotationLiterals);

    }

}
