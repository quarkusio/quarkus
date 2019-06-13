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

package io.quarkus.deployment.builditem.substrate;

import org.jboss.jandex.IndexView;
import org.jboss.jandex.Type;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Attempts to register a complete type hierarchy for reflection.
 * <p>
 * This is intended to be used to register types that are going to be serialized,
 * e.g. by Jackson or some other JSON mapper.
 * <p>
 * This will do 'smart discovery' and in addition to registering the type itself it will also attempt to
 * register the following:
 * <p>
 * - Superclasses
 * - Component types of collections
 * - Types used in bean properties if (if method reflection is enabled)
 * - Field types (if field reflection is enabled)
 * <p>
 * This discovery is applied recursively, so any additional types that are registered will also have their dependencies
 * discovered
 */
public final class ReflectiveHierarchyBuildItem extends MultiBuildItem {

    private final Type type;
    private IndexView index;

    public ReflectiveHierarchyBuildItem(Type type) {
        this.type = type;
    }

    public ReflectiveHierarchyBuildItem(Type type, IndexView index) {
        this.type = type;
        this.index = index;
    }

    public Type getType() {
        return type;
    }

    public IndexView getIndex() {
        return index;
    }
}
