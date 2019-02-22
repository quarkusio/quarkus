/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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
package io.quarkus.creator.phase.curate;

/**
 * Indicates what should be used as the source of application dependencies.
 *
 * @author Alexey Loubyansky
 */
public enum DependenciesOrigin {

    APPLICATION("application"), LAST_UPDATE("last-update"), UNKNOWN(null);

    private final String name;

    static DependenciesOrigin of(String name) {
        if (APPLICATION.name.equals(name)) {
            return APPLICATION;
        }
        if (LAST_UPDATE.name.equals(name)) {
            return LAST_UPDATE;
        }
        return UNKNOWN;
    }

    DependenciesOrigin(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String toString() {
        return name;
    }
}