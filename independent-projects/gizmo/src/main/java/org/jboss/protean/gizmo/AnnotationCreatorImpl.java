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

import java.util.HashMap;
import java.util.Map;

class AnnotationCreatorImpl implements AnnotationCreator {

    private Map<String, Object> values = new HashMap<>();
    private final String annotationType;

    AnnotationCreatorImpl(String annotationType) {
        this.annotationType = annotationType;
    }

    @Override
    public void addValue(String name, Object value) {
        values.put(name, value);
    }

    public Map<String, Object> getValues() {
        return values;
    }

    public String getAnnotationType() {
        return annotationType;
    }
}
