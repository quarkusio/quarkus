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

    public ReflectiveClassBuildItem(boolean methods, boolean fields, String... className) {
        for (String i : className) {
            if (i == null) {
                throw new NullPointerException();
            }
        }
        this.className = Arrays.asList(className);
        this.methods = methods;
        this.fields = fields;
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
}
