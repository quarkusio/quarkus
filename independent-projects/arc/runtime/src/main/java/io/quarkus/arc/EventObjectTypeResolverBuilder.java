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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Builds a special {@link TypeResolver} capable of resolving type variables by using a combination of two type hierarchies.
 *
 * The special resolver is only necessary for situations where the type of the event object contains an unresolved type variable
 * which cannot be resolved using
 * the selected event type because the selected event type is a subtype of the event object.
 *
 * For example:
 *
 * private Event<List<String>> event;
 *
 * event.fire(new ArrayList<String>());
 *
 * The event object type is {@link ArrayList} (raw type due to type erasure) The selected type is List<String>
 *
 * We cannot simply infer the correct type (ArrayList<String>) from the runtime type nor from the selected type. What this
 * special resolver does is that it
 * combines the following type variable assignments:
 *
 * L -> E
 *
 * L -> String
 *
 * and resolves E to String. The resolver is capable of doing it recursively for parameterized types.
 *
 * @author Jozef Hartinger
 *
 */
class EventObjectTypeResolverBuilder {

    private final Map<TypeVariable<?>, Type> selectedTypeVariables;
    private final Map<TypeVariable<?>, Type> eventTypeVariables;

    private final Map<TypeVariable<?>, Type> resolvedTypes;

    public EventObjectTypeResolverBuilder(Map<TypeVariable<?>, Type> selectedTypeVariables,
            Map<TypeVariable<?>, Type> eventTypeVariables) {
        this.selectedTypeVariables = selectedTypeVariables;
        this.eventTypeVariables = eventTypeVariables;
        this.resolvedTypes = new HashMap<TypeVariable<?>, Type>();
    }

    public TypeResolver build() {
        resolveTypeVariables();

        Map<TypeVariable<?>, Type> mergedVariables = new HashMap<TypeVariable<?>, Type>(eventTypeVariables);
        mergedVariables.putAll(selectedTypeVariables);
        mergedVariables.putAll(resolvedTypes);

        return new TypeResolver(mergedVariables);
    }

    protected void resolveTypeVariables() {
        for (Entry<TypeVariable<?>, Type> entry : eventTypeVariables.entrySet()) {
            // the event object does not have this variable resolved
            TypeVariable<?> key = entry.getKey();
            Type typeWithTypeVariables = entry.getValue();
            Type value = selectedTypeVariables.get(key);
            if (value == null) {
                continue;
            }
            resolveTypeVariables(typeWithTypeVariables, value);
        }
    }

    protected void resolveTypeVariables(Type type1, Type type2) {
        if (type1 instanceof TypeVariable<?>) {
            resolveTypeVariables((TypeVariable<?>) type1, type2);
        }
        if (type1 instanceof ParameterizedType) {
            resolveTypeVariables((ParameterizedType) type1, type2);
        }
    }

    protected void resolveTypeVariables(TypeVariable<?> type1, Type type2) {
        if (type2 instanceof TypeVariable) {
            // we cannot resolve this
            return;
        }
        resolvedTypes.put(type1, type2);
    }

    protected void resolveTypeVariables(ParameterizedType type1, Type type2) {
        if (type2 instanceof ParameterizedType) {
            Type[] type1Arguments = type1.getActualTypeArguments();
            Type[] type2Arguments = ((ParameterizedType) type2).getActualTypeArguments();
            if (type1Arguments.length == type2Arguments.length) {
                for (int i = 0; i < type1Arguments.length; i++) {
                    resolveTypeVariables(type1Arguments[i], type2Arguments[i]);
                }
            }
        }
    }

    public Map<TypeVariable<?>, Type> getResolvedTypes() {
        return resolvedTypes;
    }
}
