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

package io.quarkus.arc;

import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.lang.reflect.Type;
import java.util.Set;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Instance;

/**
 *
 * @author Martin Kouba
 */
public class InstanceProvider<T> implements InjectableReferenceProvider<Instance<T>> {

    private final Type requiredType;
    private final Set<Annotation> qualifiers;
    private final InjectableBean<?> targetBean;
    private final Set<Annotation> annotations;
    private final Member javaMember;
    private final int position;

    public InstanceProvider(Type type, Set<Annotation> qualifiers, InjectableBean<?> targetBean, Set<Annotation> annotations,
            Member javaMember, int position) {
        this.requiredType = type;
        this.qualifiers = qualifiers;
        this.targetBean = targetBean;
        this.annotations = annotations;
        this.javaMember = javaMember;
        this.position = position;
    }

    @Override
    public Instance<T> get(CreationalContext<Instance<T>> creationalContext) {
        return new InstanceImpl<T>(targetBean, requiredType, qualifiers, CreationalContextImpl.unwrap(creationalContext),
                annotations, javaMember, position);
    }

}
