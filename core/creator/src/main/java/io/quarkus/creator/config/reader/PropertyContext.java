/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quarkus.creator.config.reader;

/**
 *
 * @author Alexey Loubyansky
 */
public class PropertyContext {

    final PropertyContext parent;
    final String mappedName;
    final int mappedNameElements;
    final String[] nameEls;
    final PropertiesHandler<?> handler;
    Object o;

    PropertyLine nestedProperty;
    int nameElement;

    PropertyContext(PropertyContext parent, String mappedName, int mappedNameElements, String[] nameEls,
            PropertiesHandler<?> handler) {
        this.parent = parent;
        this.mappedName = mappedName;
        this.mappedNameElements = mappedNameElements;
        this.nameEls = nameEls;
        this.handler = handler;
    }

    public String getRelativeName() {
        return nestedProperty.getRelativeName(nameElement);
    }

    public String getValue() {
        return nestedProperty.getValue();
    }

    public PropertyLine getLine() {
        return nestedProperty;
    }
}
