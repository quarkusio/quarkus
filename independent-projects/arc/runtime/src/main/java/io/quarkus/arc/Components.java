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
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public final class Components {

    private final Collection<InjectableBean<?>> beans;
    private final Collection<InjectableObserverMethod<?>> observers;
    private final Collection<InjectableContext> contexts;
    private final Map<Class<? extends Annotation>, Set<Annotation>> transitiveInterceptorBindings;

    public Components(Collection<InjectableBean<?>> beans, Collection<InjectableObserverMethod<?>> observers,
            Collection<InjectableContext> contexts,
            Map<Class<? extends Annotation>, Set<Annotation>> transitiveInterceptorBindings) {
        this.beans = beans;
        this.observers = observers;
        this.contexts = contexts;
        this.transitiveInterceptorBindings = transitiveInterceptorBindings;
    }

    public Collection<InjectableBean<?>> getBeans() {
        return beans;
    }

    public Collection<InjectableObserverMethod<?>> getObservers() {
        return observers;
    }

    public Collection<InjectableContext> getContexts() {
        return contexts;
    }

    public Map<Class<? extends Annotation>, Set<Annotation>> getTransitiveInterceptorBindings() {
        return transitiveInterceptorBindings;
    }

}
