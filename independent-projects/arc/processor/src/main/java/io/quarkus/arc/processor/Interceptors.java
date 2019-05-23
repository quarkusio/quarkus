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
import javax.enterprise.inject.spi.DefinitionException;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;

final class Interceptors {

    private Interceptors() {
    }

    /**
     *
     * @param interceptorClass
     * @param beanDeployment
     * @return a new interceptor info
     */
    static InterceptorInfo createInterceptor(ClassInfo interceptorClass, BeanDeployment beanDeployment,
            InjectionPointModifier transformer, AnnotationStore store) {
        Set<AnnotationInstance> bindings = new HashSet<>();
        Integer priority = 0;
        for (AnnotationInstance annotation : store.getAnnotations(interceptorClass)) {
            if (beanDeployment.getInterceptorBinding(annotation.name()) != null) {
                bindings.add(annotation);
                // can also be a transitive binding
                Set<AnnotationInstance> transitiveInterceptorBindings = beanDeployment
                        .getTransitiveInterceptorBindings(annotation.name());
                if (transitiveInterceptorBindings != null) {
                    bindings.addAll(transitiveInterceptorBindings);
                }
            } else if (annotation.name().equals(DotNames.PRIORITY)) {
                priority = annotation.value().asInt();
            }
        }
        if (bindings.isEmpty()) {
            throw new DefinitionException("Interceptor has no bindings: " + interceptorClass);
        }
        return new InterceptorInfo(interceptorClass, beanDeployment, bindings,
                Injection.forBean(interceptorClass, null, beanDeployment, transformer), priority);
    }

}
