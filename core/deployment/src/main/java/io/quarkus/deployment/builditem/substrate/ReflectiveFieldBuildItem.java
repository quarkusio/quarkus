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

import java.lang.reflect.Field;

import org.jboss.builder.item.MultiBuildItem;
import org.jboss.jandex.FieldInfo;

public final class ReflectiveFieldBuildItem extends MultiBuildItem {

    final String declaringClass;
    final String name;

    public ReflectiveFieldBuildItem(FieldInfo field) {
        this.name = field.name();
        this.declaringClass = field.declaringClass().name().toString();
    }

    public ReflectiveFieldBuildItem(Field field) {
        this.name = field.getName();
        this.declaringClass = field.getDeclaringClass().getName();
    }

    public String getDeclaringClass() {
        return declaringClass;
    }

    public String getName() {
        return name;
    }
}
