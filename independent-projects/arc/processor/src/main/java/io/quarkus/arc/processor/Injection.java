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

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;
import org.jboss.logging.Logger;

/**
 * Injection abstraction, basically a collection of injection points plus the annotation target:
 * <ul>
 * <li>an injected field,</li>
 * <li>a bean constructor,</li>
 * <li>an initializer method,</li>
 * <li>a producer method,</li>
 * <li>a disposer method,</li>
 * <li>an observer method.</li>
 * </ul>
 *
 * @author Martin Kouba
 */
public class Injection {

    private static final Logger LOGGER = Logger.getLogger(Injection.class);

    /**
     *
     * @param beanTarget
     * @param beanDeployment
     * @return the list of injections
     */
    static List<Injection> forBean(AnnotationTarget beanTarget, BeanDeployment beanDeployment) {
        if (Kind.CLASS.equals(beanTarget.kind())) {
            List<Injection> injections = new ArrayList<>();
            forClassBean(beanTarget.asClass(), beanDeployment, injections, true);
            return injections;
        } else if (Kind.METHOD.equals(beanTarget.kind())) {
            if (beanTarget.asMethod().parameters().isEmpty()) {
                return Collections.emptyList();
            }
            // All parameters are injection points
            return Collections.singletonList(
                    new Injection(beanTarget.asMethod(), InjectionPointInfo.fromMethod(beanTarget.asMethod(), beanDeployment)));
        }
        throw new IllegalArgumentException("Unsupported annotation target");
    }

    private static void forClassBean(ClassInfo beanTarget, BeanDeployment beanDeployment, List<Injection> injections,
            boolean isFirstLevel) {

        List<AnnotationInstance> injectAnnotations = getAllInjectionPoints(beanDeployment, beanTarget, DotNames.INJECT);

        for (AnnotationInstance injectAnnotation : injectAnnotations) {
            AnnotationTarget injectTarget = injectAnnotation.target();
            switch (injectAnnotation.target().kind()) {
                case FIELD:
                    injections
                            .add(new Injection(injectTarget, Collections
                                    .singletonList(InjectionPointInfo.fromField(injectTarget.asField(), beanDeployment))));
                    break;
                case METHOD:
                    injections.add(new Injection(injectTarget,
                            InjectionPointInfo.fromMethod(injectTarget.asMethod(), beanDeployment)));
                    break;
                default:
                    LOGGER.warn("Unsupported @Inject target ignored: " + injectAnnotation.target());
                    continue;
            }
        }
        // if the class has a single non no-arg constructor that is not annotated with @Inject,
        // the class is not a non-static inner or and it not a superclass of of a bean
        // we consider that constructor as an injection
        if (isFirstLevel) {
            boolean constrInjectionExists = false;
            for (Injection injection : injections) {
                if (injection.isConstructor()) {
                    constrInjectionExists = true;
                    break;
                }
            }

            final boolean isNonStaticInnerClass = beanTarget.name().isInner()
                    && !Modifier.isStatic(beanTarget.flags());
            if (!isNonStaticInnerClass && !constrInjectionExists) {
                List<MethodInfo> nonNoargConstrs = new ArrayList<>();
                for (MethodInfo constr : beanTarget.methods()) {
                    if (Methods.INIT.equals(constr.name()) && constr.parameters().size() > 0) {
                        nonNoargConstrs.add(constr);
                    }
                }
                if (nonNoargConstrs.size() == 1) {
                    final MethodInfo injectTarget = nonNoargConstrs.get(0);
                    injections.add(new Injection(injectTarget,
                            InjectionPointInfo.fromMethod(injectTarget.asMethod(), beanDeployment)));
                }
            }
        }

        for (DotName resourceAnnotation : beanDeployment.getResourceAnnotations()) {
            List<AnnotationInstance> resourceAnnotations = getAllInjectionPoints(beanDeployment, beanTarget,
                    resourceAnnotation);
            if (resourceAnnotations != null) {
                for (AnnotationInstance resourceAnnotationInstance : resourceAnnotations) {
                    if (Kind.FIELD == resourceAnnotationInstance.target().kind()
                            && resourceAnnotationInstance.target().asField().annotations().stream()
                                    .noneMatch(a -> DotNames.INJECT.equals(a.name()))) {
                        // Add special injection for a resource field
                        injections.add(new Injection(resourceAnnotationInstance.target(), Collections
                                .singletonList(InjectionPointInfo
                                        .fromResourceField(resourceAnnotationInstance.target().asField(), beanDeployment))));
                    }
                    // TODO setter injection
                }
            }
        }

        if (!beanTarget.superName().equals(DotNames.OBJECT)) {
            ClassInfo info = beanDeployment.getIndex().getClassByName(beanTarget.superName());
            if (info != null) {
                forClassBean(info, beanDeployment, injections, false);
            }
        }

    }

    static Injection forDisposer(MethodInfo disposerMethod, BeanDeployment beanDeployment) {
        return new Injection(disposerMethod, InjectionPointInfo.fromMethod(disposerMethod, beanDeployment,
                annotations -> annotations.stream().anyMatch(a -> a.name().equals(DotNames.DISPOSES))));
    }

    static Injection forObserver(MethodInfo observerMethod, BeanDeployment beanDeployment) {
        return new Injection(observerMethod, InjectionPointInfo.fromMethod(observerMethod, beanDeployment,
                annotations -> annotations.stream()
                        .anyMatch(a -> a.name().equals(DotNames.OBSERVES) || a.name().equals(DotNames.OBSERVES_ASYNC))));
    }

    final AnnotationTarget target;

    final List<InjectionPointInfo> injectionPoints;

    public Injection(AnnotationTarget target, List<InjectionPointInfo> injectionPoints) {
        this.target = target;
        this.injectionPoints = injectionPoints;
    }

    boolean isMethod() {
        return Kind.METHOD == target.kind();
    }

    boolean isConstructor() {
        return isMethod() && target.asMethod().name().equals(Methods.INIT);
    }

    boolean isField() {
        return Kind.FIELD == target.kind();
    }

    private static List<AnnotationInstance> getAllInjectionPoints(BeanDeployment beanDeployment, ClassInfo beanClass,
            DotName name) {
        List<AnnotationInstance> injectAnnotations = new ArrayList<>();
        for (FieldInfo field : beanClass.fields()) {
            AnnotationInstance inject = beanDeployment.getAnnotation(field, name);
            if (inject != null) {
                injectAnnotations.add(inject);
            }
        }
        for (MethodInfo method : beanClass.methods()) {
            AnnotationInstance inject = beanDeployment.getAnnotation(method, name);
            if (inject != null) {
                injectAnnotations.add(inject);
            }
        }
        return injectAnnotations;
    }

}
