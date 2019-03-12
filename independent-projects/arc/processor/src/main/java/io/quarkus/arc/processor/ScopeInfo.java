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

package io.quarkus.arc.processor;

import java.lang.annotation.Annotation;
import java.util.Objects;

import org.jboss.jandex.DotName;

public class ScopeInfo {

    private final DotName dotName;

    private final boolean isNormal;

    ScopeInfo(Class<? extends Annotation> clazz, boolean isNormal) {
        this.dotName = DotName.createSimple(clazz.getName());
        this.isNormal = isNormal;
    }

    public DotName getDotName() {
        return dotName;
    }

    public boolean isNormal() {
        return isNormal;
    }

    @Override
    public int hashCode() {
        return Objects.hash(dotName);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ScopeInfo other = (ScopeInfo) obj;
        return Objects.equals(dotName, other.dotName);
    }

}
