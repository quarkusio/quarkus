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

package org.jboss.quarkus.arc;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Event;

/**
 *
 * @author Martin Kouba
 */
public class EventProvider<T> implements InjectableReferenceProvider<Event<T>> {

    private final Type eventType;

    private final Set<Annotation> eventQualifiers;

    public EventProvider(Type eventType, Set<Annotation> eventQualifiers) {
        this.eventType = eventType;
        this.eventQualifiers = eventQualifiers;
    }

    @Override
    public Event<T> get(CreationalContext<Event<T>> creationalContext) {
        return new EventImpl<>(eventType, eventQualifiers);
    }

}
