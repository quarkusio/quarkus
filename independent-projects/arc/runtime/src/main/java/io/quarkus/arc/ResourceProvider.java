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
import java.lang.reflect.Type;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;

/**
 * Represents a placeholder for all suppored non-CDI injection points.
 *
 * @author Martin Kouba
 * @see ResourceReferenceProvider
 */
public class ResourceProvider implements InjectableReferenceProvider<Object> {

    private final Type type;

    private final Set<Annotation> annotations;

    public ResourceProvider(Type type, Set<Annotation> annotations) {
        this.type = type;
        this.annotations = annotations;
    }

    @Override
    public Object get(CreationalContext<Object> creationalContext) {
        InstanceHandle<Object> instance = ArcContainerImpl.instance().getResource(type, annotations);
        if (instance != null) {
            CreationalContextImpl<?> ctx = CreationalContextImpl.unwrap(creationalContext);
            if (ctx.getParent() != null) {
                ctx.getParent().addDependentInstance(instance);
            }
            return instance.get();
        }
        // TODO log a warning that a resource cannot be injected
        return null;
    }

}
