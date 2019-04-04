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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import javax.enterprise.inject.spi.DefinitionException;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.MethodParameterInfo;
import org.jboss.jandex.Type;

/**
 *
 * @author Martin Kouba
 */
public class DisposerInfo {

    private final BeanInfo declaringBean;

    private final MethodInfo disposerMethod;

    private final Injection injection;

    private final MethodParameterInfo disposedParameter;

    DisposerInfo(BeanInfo declaringBean, MethodInfo disposerMethod, Injection injection) {
        this.declaringBean = declaringBean;
        this.disposerMethod = disposerMethod;
        this.injection = injection;
        this.disposedParameter = initDisposedParam(disposerMethod);
    }

    public BeanInfo getDeclaringBean() {
        return declaringBean;
    }

    public MethodInfo getDisposerMethod() {
        return disposerMethod;
    }

    public MethodParameterInfo getDisposedParameter() {
        return disposedParameter;
    }

    Injection getInjection() {
        return injection;
    }

    public List<InjectionPointInfo> getAllInjectionPoints() {
        List<InjectionPointInfo> injectionPoints = new ArrayList<>();
        injectionPoints.addAll(injection.injectionPoints);
        return injectionPoints;
    }

    void init(List<Throwable> errors) {
        for (InjectionPointInfo injectionPoint : injection.injectionPoints) {
            Beans.resolveInjectionPoint(declaringBean.getDeployment(), null, injectionPoint, errors);
        }
    }

    Collection<AnnotationInstance> getDisposedParameterQualifiers() {
        return Annotations.getParameterAnnotations(declaringBean.getDeployment(), disposerMethod, disposedParameter.position())
                .stream().filter(a -> declaringBean.getDeployment().getQualifier(a.name()) != null)
                .collect(Collectors.toList());
    }

    Type getDisposedParameterType() {
        return disposerMethod.parameters().get(disposedParameter.position());
    }

    MethodParameterInfo initDisposedParam(MethodInfo disposerMethod) {
        List<MethodParameterInfo> disposedParams = new ArrayList<>();
        for (AnnotationInstance annotation : disposerMethod.annotations()) {
            if (Kind.METHOD_PARAMETER.equals(annotation.target().kind()) && annotation.name().equals(DotNames.DISPOSES)) {
                disposedParams.add(annotation.target().asMethodParameter());
            }
        }
        if (disposedParams.isEmpty()) {
            throw new DefinitionException("No disposed parameters found for " + disposerMethod);
        } else if (disposedParams.size() > 1) {
            throw new DefinitionException("Multiple disposed parameters found for " + disposerMethod);
        }
        return disposedParams.get(0);
    }
}
