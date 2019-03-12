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

package io.quarkus.spring.di.deployment;

import static org.jboss.jandex.AnnotationInstance.create;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;

import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.arc.processor.ScopeInfo;
import io.quarkus.arc.processor.Transformation;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

/*
 * A simple processor that maps annotations Spring DI annotation to CDI annotation
 * Arc's handling of annotation mapping (by creating an extra abstraction layer on top of the Jandex index)
 * suits this sort of handling perfectly
 */
public class SpringDIProcessor {

    private static final DotName SPRING_SCOPE_ANNOTATION = DotName.createSimple("org.springframework.context.annotation.Scope");

    private static final DotName[] SPRING_STEREOTYPE_ANNOTATIONS = {
            DotName.createSimple("org.springframework.stereotype.Component"),
            DotName.createSimple("org.springframework.stereotype.Service"),
            DotName.createSimple("org.springframework.stereotype.Repository"),
    };

    private static final DotName CONFIGURATION_ANNOTATION = DotName
            .createSimple("org.springframework.context.annotation.Configuration");

    private static final DotName BEAN_ANNOTATION = DotName.createSimple("org.springframework.context.annotation.Bean");

    private static final DotName AUTOWIRED_ANNOTATION = DotName
            .createSimple("org.springframework.beans.factory.annotation.Autowired");

    private static final DotName SPRING_QUALIFIER_ANNOTATION = DotName
            .createSimple("org.springframework.beans.factory.annotation.Qualifier");

    private static final DotName VALUE_ANNOTATION = DotName.createSimple("org.springframework.beans.factory.annotation.Value");

    private static final DotName CDI_SINGLETON_ANNOTATION = BuiltinScope.SINGLETON.getInfo().getDotName();
    private static final DotName CDI_DEPENDENT_ANNOTATION = BuiltinScope.DEPENDENT.getInfo().getDotName();
    private static final DotName CDI_APP_SCOPED_ANNOTATION = BuiltinScope.APPLICATION.getInfo().getDotName();
    private static final DotName CDI_NAMED_ANNOTATION = DotNames.NAMED;
    private static final DotName CDI_INJECT_ANNOTATION = DotNames.INJECT;
    private static final DotName CDI_PRODUCES_ANNOTATION = DotNames.PRODUCES;
    private static final DotName MP_CONFIG_PROPERTY_ANNOTATION = DotName.createSimple(ConfigProperty.class.getName());

    @BuildStep
    FeatureBuildItem registerFeature() {
        return new FeatureBuildItem(FeatureBuildItem.SPRING_DI);
    }

    @BuildStep
    AnnotationsTransformerBuildItem beanTransformer() {
        return new AnnotationsTransformerBuildItem(new AnnotationsTransformer() {
            @Override
            public void transform(TransformationContext context) {
                if (context.getAnnotations().isEmpty()) {
                    return;
                }

                final Set<AnnotationInstance> annotationsToAdd = new HashSet<>();

                //if it's a class, it's a Bean or a Bean producer
                if (context.isClass()) {
                    final ClassInfo classInfo = context.getTarget().asClass();
                    for (DotName springStereotypeAnnotation : SPRING_STEREOTYPE_ANNOTATIONS) {
                        if (classInfo.annotations().containsKey(springStereotypeAnnotation)) {
                            //we use Singleton as the default scope (since that's what Spring DI does)
                            //but will switch to Dependent if the is a Scope annotation with the value of prototype
                            DotName cdiAnnotation = CDI_SINGLETON_ANNOTATION;
                            if (classInfo.annotations().containsKey(SPRING_SCOPE_ANNOTATION)) {
                                final AnnotationInstance annotation = classInfo.classAnnotation(SPRING_SCOPE_ANNOTATION);
                                final AnnotationValue springScopeAnnotationValue = annotation.value();
                                if (springScopeAnnotationValue != null &&
                                        "prototype".equals(springScopeAnnotationValue.asString())) {
                                    cdiAnnotation = CDI_DEPENDENT_ANNOTATION;
                                }
                            }
                            annotationsToAdd.add(create(
                                    cdiAnnotation,
                                    context.getTarget(),
                                    new ArrayList<>()));

                            //check if the spring annotation defines a name for the bean
                            final AnnotationValue beanNameAnnotationValue = classInfo
                                    .classAnnotation(springStereotypeAnnotation).value();
                            addCDINamedAnnotation(context, beanNameAnnotationValue, annotationsToAdd);

                            break;
                        }
                    }

                    if (classInfo.annotations().containsKey(CONFIGURATION_ANNOTATION)) {
                        annotationsToAdd.add(create(
                                CDI_APP_SCOPED_ANNOTATION,
                                context.getTarget(),
                                new ArrayList<>()));

                    }
                } else if (context.isField()) { // here we check for @Autowired and @Value annotations
                    final FieldInfo fieldInfo = context.getTarget().asField();
                    if (fieldInfo.hasAnnotation(AUTOWIRED_ANNOTATION)) {
                        annotationsToAdd.add(create(
                                CDI_INJECT_ANNOTATION,
                                context.getTarget(),
                                new ArrayList<>()));

                        if (fieldInfo.hasAnnotation(SPRING_QUALIFIER_ANNOTATION)) {
                            final AnnotationInstance annotation = fieldInfo.annotation(SPRING_QUALIFIER_ANNOTATION);
                            final AnnotationValue annotationValue = annotation.value();
                            if (annotationValue != null) {
                                final String value = annotationValue.asString();
                                annotationsToAdd.add(create(
                                        CDI_NAMED_ANNOTATION,
                                        context.getTarget(),
                                        new ArrayList<AnnotationValue>() {
                                            {
                                                add(AnnotationValue.createStringValue("value", value));
                                            }
                                        }));
                            }
                        }
                    } else if (fieldInfo.hasAnnotation(VALUE_ANNOTATION)) {
                        final AnnotationInstance annotation = fieldInfo.annotation(VALUE_ANNOTATION);
                        final AnnotationValue annotationValue = annotation.value();
                        if (annotationValue != null) {
                            String defaultValue = null;
                            String propertyName = annotationValue.asString().replace("${", "").replace("}", "");
                            if (propertyName.contains(":")) {
                                final int index = propertyName.indexOf(':');
                                if (index < propertyName.length() - 1) {
                                    defaultValue = propertyName.substring(index + 1);
                                }
                                propertyName = propertyName.substring(0, index);

                            }
                            final List<AnnotationValue> annotationValues = new ArrayList<>();
                            annotationValues.add(AnnotationValue.createStringValue("name", propertyName));
                            if (defaultValue != null && !defaultValue.isEmpty()) {
                                annotationValues.add(AnnotationValue.createStringValue("defaultValue", defaultValue));
                            }
                            annotationsToAdd.add(create(
                                    MP_CONFIG_PROPERTY_ANNOTATION,
                                    context.getTarget(),
                                    annotationValues));
                            annotationsToAdd.add(create(
                                    CDI_INJECT_ANNOTATION,
                                    context.getTarget(),
                                    new ArrayList<>()));
                        }
                    }
                } else if (context.isMethod()) {
                    final MethodInfo methodInfo = context.getTarget().asMethod();
                    if (methodInfo.hasAnnotation(BEAN_ANNOTATION)) {
                        annotationsToAdd.add(create(
                                CDI_PRODUCES_ANNOTATION,
                                context.getTarget(),
                                new ArrayList<>()));
                        annotationsToAdd.add(create(
                                CDI_DEPENDENT_ANNOTATION,
                                context.getTarget(),
                                new ArrayList<>()));

                        //check if the spring annotation defines a name for the bean
                        final AnnotationValue beanNameAnnotationValue = methodInfo.annotation(BEAN_ANNOTATION).value("name");
                        final AnnotationValue beanValueAnnotationValue = methodInfo.annotation(BEAN_ANNOTATION).value("value");
                        if (!addCDINamedAnnotation(context, beanNameAnnotationValue, annotationsToAdd)) {
                            addCDINamedAnnotation(context, beanValueAnnotationValue, annotationsToAdd);
                        }
                    }

                    // add method parameter conversion annotations
                    for (AnnotationInstance annotation : methodInfo.annotations()) {
                        if (annotation.target().kind() == AnnotationTarget.Kind.METHOD_PARAMETER
                                && annotation.name().equals(SPRING_QUALIFIER_ANNOTATION)) {
                            final AnnotationValue annotationValue = annotation.value();
                            if (annotationValue != null) {
                                final String value = annotationValue.asString();
                                annotationsToAdd.add(create(
                                        CDI_NAMED_ANNOTATION,
                                        annotation.target(),
                                        Collections.singletonList(AnnotationValue.createStringValue("value", value))));
                            }
                        }
                    }

                }

                if (!annotationsToAdd.isEmpty()) {
                    final Transformation transform = context.transform();
                    for (AnnotationInstance annotationInstance : annotationsToAdd) {
                        transform.add(annotationInstance);
                    }
                    transform.done();
                }
            }

            private boolean addCDINamedAnnotation(TransformationContext context,
                    AnnotationValue annotationValue,
                    Set<AnnotationInstance> annotationsToAdd) {
                if (annotationValue == null) {
                    return false;
                }

                final String beanName = determineName(annotationValue);
                if (beanName != null && !"".equals(beanName)) {
                    annotationsToAdd.add(create(
                            CDI_NAMED_ANNOTATION,
                            context.getTarget(),
                            Collections.singletonList(AnnotationValue.createStringValue("value", beanName))));

                    return true;
                }

                return false;
            }

            private String determineName(AnnotationValue annotationValue) {
                if (annotationValue.kind() == AnnotationValue.Kind.ARRAY) {
                    return annotationValue.asStringArray()[0];
                } else if (annotationValue.kind() == AnnotationValue.Kind.STRING) {
                    return annotationValue.asString();
                }
                return null;
            }
        });
    }
}
