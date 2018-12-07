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

package org.jboss.shamrock.beanvalidation;

import static org.jboss.shamrock.annotations.ExecutionTime.STATIC_INIT;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.validation.Constraint;

import org.hibernate.validator.internal.metadata.core.ConstraintHelper;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.shamrock.annotations.BuildProducer;
import org.jboss.shamrock.annotations.BuildStep;
import org.jboss.shamrock.annotations.Record;
import org.jboss.shamrock.beanvalidation.runtime.ValidatorProvider;
import org.jboss.shamrock.beanvalidation.runtime.ValidatorTemplate;
import org.jboss.shamrock.deployment.builditem.AdditionalBeanBuildItem;
import org.jboss.shamrock.deployment.builditem.CombinedIndexBuildItem;
import org.jboss.shamrock.deployment.builditem.HotDeploymentConfigFileBuildItem;
import org.jboss.shamrock.deployment.builditem.InjectionFactoryBuildItem;
import org.jboss.shamrock.deployment.builditem.substrate.ReflectiveClassBuildItem;
import org.jboss.shamrock.deployment.builditem.substrate.ReflectiveFieldBuildItem;
import org.jboss.shamrock.deployment.builditem.substrate.ReflectiveMethodBuildItem;
import org.jboss.shamrock.deployment.builditem.substrate.SubstrateConfigBuildItem;
import org.jboss.shamrock.deployment.recording.RecorderContext;

class BeanValidationProcessor {


    @BuildStep
    HotDeploymentConfigFileBuildItem configFile() {
        return new HotDeploymentConfigFileBuildItem("META-INF/validation.xml");
    }


    @BuildStep
    AdditionalBeanBuildItem registerBean() {
        return new AdditionalBeanBuildItem(ValidatorProvider.class);
    }

    @BuildStep
    @Record(STATIC_INIT)
    public void build(ValidatorTemplate template, RecorderContext recorder, InjectionFactoryBuildItem factory,
                      BuildProducer<ReflectiveFieldBuildItem> reflectiveFields,
                      BuildProducer<ReflectiveMethodBuildItem> reflectiveMethods,
                      CombinedIndexBuildItem combinedIndexBuildItem,
                      BuildProducer<ReflectiveClassBuildItem> reflectiveClass) throws Exception {

        IndexView indexView = combinedIndexBuildItem.getIndex();

        Set<DotName> constraintAnnotations = new HashSet<>();

        // Collect the constraint annotations provided by Hibernate Validator and Bean Validation
        contributeBuiltinConstraints(constraintAnnotations);

        // Add the constraint annotations present in the application itself
        for (AnnotationInstance constraint : indexView.getAnnotations(DotName.createSimple(Constraint.class.getName()))) {
            constraintAnnotations.add(constraint.target().asClass().name());
        }

        Set<Class<?>> classesToBeValidated = new HashSet<>();

        for (DotName constraint : constraintAnnotations) {
            Collection<AnnotationInstance> annotationInstances = indexView.getAnnotations(constraint);

            for (AnnotationInstance annotation : annotationInstances) {
                if (annotation.target().kind() == AnnotationTarget.Kind.FIELD) {
                    contributeClass(classesToBeValidated, recorder, indexView, annotation.target().asField().declaringClass());
                    reflectiveFields.produce(new ReflectiveFieldBuildItem(annotation.target().asField()));
                } else if (annotation.target().kind() == AnnotationTarget.Kind.METHOD) {
                    contributeClass(classesToBeValidated, recorder, indexView, annotation.target().asMethod().declaringClass());
                    reflectiveMethods.produce(new ReflectiveMethodBuildItem(annotation.target().asMethod()));
                } else if (annotation.target().kind() == AnnotationTarget.Kind.METHOD_PARAMETER) {
                    contributeClass(classesToBeValidated, recorder, indexView, annotation.target().asMethodParameter().method().declaringClass());
                    reflectiveMethods.produce(new ReflectiveMethodBuildItem(annotation.target().asMethodParameter().method()));
                } else if (annotation.target().kind() == AnnotationTarget.Kind.CLASS) {
                    contributeClass(classesToBeValidated, recorder, indexView, annotation.target().asClass());
                    reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, annotation.target().asClass().name().toString()));
                }
            }
        }

        template.initializeValidatorFactory(classesToBeValidated);
    }

    @BuildStep
    SubstrateConfigBuildItem substrateConfig() {
        return SubstrateConfigBuildItem.builder()
                .addRuntimeInitializedClass("javax.el.ELUtil")
                .addResourceBundle("org.hibernate.validator.ValidationMessages")
                .build();
    }

    private static void contributeBuiltinConstraints(Set<DotName> constraintCollector) {
        Set<Class<? extends Annotation>> builtinConstraints = new ConstraintHelper().getBuiltinConstraints();
        for (Class<? extends Annotation> builtinConstraint : builtinConstraints) {
            constraintCollector.add(DotName.createSimple(builtinConstraint.getName()));
        }
    }

    private static void contributeClass(Set<Class<?>> classCollector, RecorderContext recorder, IndexView indexView, ClassInfo classInfo) {
        classCollector.add(recorder.classProxy(classInfo.name().toString()));
        for (ClassInfo subclass : indexView.getAllKnownSubclasses(classInfo.name())) {
            if (Modifier.isAbstract(subclass.flags())) {
                // we can avoid adding the abstract classes here: either they are parent classes
                // and they will be dealt with by Hibernate Validator or they are child classes
                // without any proper implementation and we can ignore them.
                continue;
            }
            classCollector.add(recorder.classProxy(subclass.name().toString()));
        }
    }
}
