/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quarkus.arc.processor;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import io.quarkus.arc.BeanManagerProvider;
import io.quarkus.arc.BeanMetadataProvider;
import io.quarkus.arc.EventProvider;
import io.quarkus.arc.InjectableReferenceProvider;
import io.quarkus.arc.InjectionPointProvider;
import io.quarkus.arc.InstanceProvider;
import io.quarkus.arc.ResourceProvider;
import io.quarkus.arc.processor.InjectionPointInfo.InjtetionPointKind;
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
        @Override
        void generate(ClassOutput classOutput, BeanDeployment beanDeployment, InjectionPointInfo injectionPoint, ClassCreator clazzCreator,
                MethodCreator constructor, String providerName, AnnotationLiteralProcessor annotationLiterals) {

            ResultHandle qualifiers = constructor.newInstance(MethodDescriptor.ofConstructor(HashSet.class));
            if (!injectionPoint.getRequiredQualifiers().isEmpty()) {
                // Set<Annotation> instanceProvider1Qualifiers = new HashSet<>()
                // instanceProvider1Qualifiers.add(javax.enterprise.inject.Default.Literal.INSTANCE)

                for (AnnotationInstance qualifierAnnotation : injectionPoint.getRequiredQualifiers()) {
                    BuiltinQualifier qualifier = BuiltinQualifier.of(qualifierAnnotation);
                    if (qualifier != null) {
                        constructor.invokeInterfaceMethod(MethodDescriptors.SET_ADD, qualifiers, qualifier.getLiteralInstance(constructor));
                    } else {
                        // Create annotation literal first
                        ClassInfo qualifierClass = beanDeployment.getQualifier(qualifierAnnotation.name());
                        constructor.invokeInterfaceMethod(MethodDescriptors.SET_ADD, qualifiers, annotationLiterals.process(constructor, classOutput,
                                qualifierClass, qualifierAnnotation, Types.getPackageName(clazzCreator.getClassName())));
                    }
                }
            }
            ResultHandle parameterizedType = Types.getTypeHandle(constructor, injectionPoint.getRequiredType());
            ResultHandle instanceProvider = constructor.newInstance(
                    MethodDescriptor.ofConstructor(InstanceProvider.class, java.lang.reflect.Type.class, Set.class), parameterizedType, qualifiers);
            constructor.writeInstanceField(FieldDescriptor.of(clazzCreator.getClassName(), providerName, InjectableReferenceProvider.class.getName()),
                    constructor.getThis(), instanceProvider);

        }
    }), INJECTION_POINT(DotNames.INJECTION_POINT, new Generator() {
        @Override
        void generate(ClassOutput classOutput, BeanDeployment beanDeployment, InjectionPointInfo injectionPoint, ClassCreator clazzCreator,
                MethodCreator constructor, String providerName, AnnotationLiteralProcessor annotationLiterals) {
            // this.injectionPointProvider1 = new InjectionPointProvider();
            constructor.writeInstanceField(FieldDescriptor.of(clazzCreator.getClassName(), providerName, InjectableReferenceProvider.class.getName()),
                    constructor.getThis(), constructor.newInstance(MethodDescriptor.ofConstructor(InjectionPointProvider.class)));
        }
    }), BEAN(DotNames.BEAN, new Generator() {
        @Override
        void generate(ClassOutput classOutput, BeanDeployment beanDeployment, InjectionPointInfo injectionPoint, ClassCreator clazzCreator,
                MethodCreator constructor, String providerName, AnnotationLiteralProcessor annotationLiterals) {
            // this.beanProvider1 = new BeanMetadataProvider<>();
            constructor.writeInstanceField(FieldDescriptor.of(clazzCreator.getClassName(), providerName, InjectableReferenceProvider.class.getName()),
                    constructor.getThis(), constructor.newInstance(MethodDescriptor.ofConstructor(BeanMetadataProvider.class)));
        }
    }), BEAN_MANAGER(DotNames.BEAN_MANAGER, new Generator() {
        @Override
        void generate(ClassOutput classOutput, BeanDeployment beanDeployment, InjectionPointInfo injectionPoint, ClassCreator clazzCreator,
                MethodCreator constructor, String providerName, AnnotationLiteralProcessor annotationLiterals) {
            constructor.writeInstanceField(FieldDescriptor.of(clazzCreator.getClassName(), providerName, InjectableReferenceProvider.class.getName()),
                    constructor.getThis(), constructor.newInstance(MethodDescriptor.ofConstructor(BeanManagerProvider.class)));
        }
    }), EVENT(DotNames.EVENT, new Generator() {
        @Override
        void generate(ClassOutput classOutput, BeanDeployment beanDeployment, InjectionPointInfo injectionPoint, ClassCreator clazzCreator,
                MethodCreator constructor, String providerName, AnnotationLiteralProcessor annotationLiterals) {

            ResultHandle qualifiers = constructor.newInstance(MethodDescriptor.ofConstructor(HashSet.class));
            if (!injectionPoint.getRequiredQualifiers().isEmpty()) {
                // Set<Annotation> instanceProvider1Qualifiers = new HashSet<>()
                // instanceProvider1Qualifiers.add(javax.enterprise.inject.Default.Literal.INSTANCE)

                for (AnnotationInstance qualifierAnnotation : injectionPoint.getRequiredQualifiers()) {
                    BuiltinQualifier qualifier = BuiltinQualifier.of(qualifierAnnotation);
                    if (qualifier != null) {
                        constructor.invokeInterfaceMethod(MethodDescriptors.SET_ADD, qualifiers, qualifier.getLiteralInstance(constructor));
                    } else {
                        // Create annotation literal first
                        ClassInfo qualifierClass = beanDeployment.getQualifier(qualifierAnnotation.name());
                        constructor.invokeInterfaceMethod(MethodDescriptors.SET_ADD, qualifiers, annotationLiterals.process(constructor, classOutput,
                                qualifierClass, qualifierAnnotation, Types.getPackageName(clazzCreator.getClassName())));
                    }
                }
            }
            ResultHandle parameterizedType = Types.getTypeHandle(constructor, injectionPoint.getRequiredType());
            ResultHandle eventProvider = constructor.newInstance(MethodDescriptor.ofConstructor(EventProvider.class, java.lang.reflect.Type.class, Set.class),
                    parameterizedType, qualifiers);
            constructor.writeInstanceField(FieldDescriptor.of(clazzCreator.getClassName(), providerName, InjectableReferenceProvider.class.getName()),
                    constructor.getThis(), eventProvider);
        }
    }), RESOURCE(DotNames.OBJECT, new Generator() {
        @Override
        void generate(ClassOutput classOutput, BeanDeployment beanDeployment, InjectionPointInfo injectionPoint, ClassCreator clazzCreator,
                MethodCreator constructor, String providerName, AnnotationLiteralProcessor annotationLiterals) {

            ResultHandle annotations = constructor.newInstance(MethodDescriptor.ofConstructor(HashSet.class));
            // For a resource field the required qualifiers contain all annotations declared on the field
            if (!injectionPoint.getRequiredQualifiers().isEmpty()) {
                for (AnnotationInstance annotation : injectionPoint.getRequiredQualifiers()) {
                    // Create annotation literal first
                    ClassInfo annotationClass = beanDeployment.getIndex().getClassByName(annotation.name());
                    constructor.invokeInterfaceMethod(MethodDescriptors.SET_ADD, annotations, annotationLiterals.process(constructor, classOutput,
                            annotationClass, annotation, Types.getPackageName(clazzCreator.getClassName())));
                }
            }
            ResultHandle parameterizedType = Types.getTypeHandle(constructor, injectionPoint.getRequiredType());
            ResultHandle resourceProvider = constructor.newInstance(
                    MethodDescriptor.ofConstructor(ResourceProvider.class, java.lang.reflect.Type.class, Set.class), parameterizedType, annotations);
            constructor.writeInstanceField(FieldDescriptor.of(clazzCreator.getClassName(), providerName, InjectableReferenceProvider.class.getName()),
                    constructor.getThis(), resourceProvider);
        }
    }, ip -> ip.getKind() == InjtetionPointKind.RESOURCE);

    private final DotName rawTypeDotName;

    private final Generator generator;

    private final Predicate<InjectionPointInfo> matcher;

    BuiltinBean(DotName rawTypeDotName, Generator generator) {
        this(rawTypeDotName, generator, ip -> ip.getKind() == InjtetionPointKind.CDI && rawTypeDotName.equals(ip.getRequiredType().name()));
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

        abstract void generate(ClassOutput classOutput, BeanDeployment beanDeployment, InjectionPointInfo injectionPoint, ClassCreator clazzCreator,
                MethodCreator constructor, String providerName, AnnotationLiteralProcessor annotationLiterals);

    }

}
