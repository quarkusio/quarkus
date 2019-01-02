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

package org.jboss.protean.gizmo;

import java.lang.reflect.Field;
import java.util.Objects;

import org.jboss.jandex.FieldInfo;

public class FieldDescriptor {

    private final String declaringClass;
    private final String name;
    private final String type;

    private FieldDescriptor(String declaringClass, String name, String type) {
        this.declaringClass = declaringClass.replace('.', '/');
        this.name = name;
        this.type = type;
    }

    private FieldDescriptor(FieldInfo fieldInfo) {
        this.name = fieldInfo.name();
        this.type = DescriptorUtils.typeToString(fieldInfo.type());
        this.declaringClass = fieldInfo.declaringClass().toString().replace('.', '/');
    }

    public static FieldDescriptor of(String declaringClass, String name, String type) {
        return new FieldDescriptor(DescriptorUtils.objectToInternalClassName(declaringClass), name, DescriptorUtils.objectToDescriptor(type));
    }

    public static FieldDescriptor of(String declaringClass, String name, Class<?> type) {
        return new FieldDescriptor(DescriptorUtils.objectToInternalClassName(declaringClass), name, DescriptorUtils.objectToDescriptor(type));
    }

    public static FieldDescriptor of(Class<?> declaringClass, String name, String type) {
        return new FieldDescriptor(DescriptorUtils.objectToInternalClassName(declaringClass), name, DescriptorUtils.objectToDescriptor(type));
    }

    public static FieldDescriptor of(Class<?> declaringClass, String name, Class<?> type) {
        return new FieldDescriptor(DescriptorUtils.objectToInternalClassName(declaringClass), name, DescriptorUtils.objectToDescriptor(type));
    }

    public static FieldDescriptor of(FieldInfo fieldInfo) {
        return new FieldDescriptor(fieldInfo);
    }

    public static FieldDescriptor of(Field field) {
        return of(field.getDeclaringClass(), field.getName(), field.getType());
    }

    public String getName() {
        return name;
    }

    public String getDeclaringClass() {
        return declaringClass;
    }

    public String getType() {
        return type;
    }

    public boolean equals(final Object obj) {
        return obj instanceof FieldDescriptor && equals((FieldDescriptor) obj);
    }

    public boolean equals(final FieldDescriptor obj) {
        return obj == this || obj != null && declaringClass.equals(obj.declaringClass) && name.equals(obj.name) && type.equals(obj.type);
    }

    public int hashCode() {
        return Objects.hash(declaringClass, name, type);
    }
}
