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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jboss.builder.item.MultiBuildItem;

/**
 * Used to register a class for reflection in substrate
 */
public final class ReflectiveClassBuildItem extends MultiBuildItem {

    private final List<String> className;
    private final boolean methods;
    private final boolean fields;
    private final boolean constructors;
    private final boolean finalIsWritable;

    public ReflectiveClassBuildItem(boolean methods, boolean fields, Class<?>... className) {
        this(true, methods, fields, className);
    }

    public ReflectiveClassBuildItem(boolean constructors, boolean methods, boolean fields, Class<?>... className) {
        this(constructors, methods, fields, false, className);
    }

    public ReflectiveClassBuildItem(boolean constructors, boolean methods, boolean fields, boolean finalIsWritable,
            Class<?>... className) {
        List<String> names = new ArrayList<>();
        for (Class<?> i : className) {
            if (i == null) {
                throw new NullPointerException();
            }
            names.add(i.getName());
        }
        this.className = names;
        this.methods = methods;
        this.fields = fields;
        this.constructors = constructors;
        this.finalIsWritable = finalIsWritable;
    }

    public ReflectiveClassBuildItem(boolean methods, boolean fields, String... className) {
        this(true, methods, fields, className);
    }

    public ReflectiveClassBuildItem(boolean constructors, boolean methods, boolean fields, String... className) {
        this(constructors, methods, fields, false, className);
    }

    public ReflectiveClassBuildItem(boolean constructors, boolean methods, boolean fields, boolean finalIsWritable,
            String... className) {
        for (String i : className) {
            if (i == null) {
                throw new NullPointerException();
            }
        }
        this.className = Arrays.asList(className);
        this.methods = methods;
        this.fields = fields;
        this.constructors = constructors;
        this.finalIsWritable = finalIsWritable;
    }

    public List<String> getClassNames() {
        return className;
    }

    public boolean isMethods() {
        return methods;
    }

    public boolean isFields() {
        return fields;
    }

    public boolean isConstructors() {
        return constructors;
    }

    public boolean isFinalWritable() {
        return finalIsWritable;
    }
}
