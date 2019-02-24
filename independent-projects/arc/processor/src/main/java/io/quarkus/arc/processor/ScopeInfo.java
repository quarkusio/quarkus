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

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.RequestScoped;
import javax.inject.Singleton;

import org.jboss.jandex.DotName;

public enum ScopeInfo {

    DEPENDENT(Dependent.class),
    SINGLETON(Singleton.class),
    APPLICATION(ApplicationScoped.class, true),
    REQUEST(RequestScoped.class, true),
    ;

    private final DotName dotName;

    private final Class<? extends Annotation> clazz;

    private final boolean isNormal;

    private ScopeInfo(Class<? extends Annotation> clazz) {
        this(clazz, false);
    }

    private ScopeInfo(Class<? extends Annotation> clazz, boolean isNormal) {
        this.dotName = DotNames.create(clazz);
        this.clazz = clazz;
        this.isNormal = isNormal;
    }

    public DotName getDotName() {
        return dotName;
    }

    public Class<? extends Annotation> getClazz() {
        return clazz;
    }

    public boolean isNormal() {
        return isNormal;
    }

    public boolean isDefault() {
        return DEPENDENT == this;
    }

    public static ScopeInfo from(DotName name) {
        for (ScopeInfo scope : ScopeInfo.values()) {
            if (scope.getDotName().equals(name)) {
                return scope;
            }
        }
        return null;
    }

}
