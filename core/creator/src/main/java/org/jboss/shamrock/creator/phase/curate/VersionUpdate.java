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
 * Indicates which update policy should be applied.
 *
 * @author Alexey Loubyansky
 */
public enum VersionUpdate {

    LATEST("latest"), NEXT("next"), NONE("none"), UNKNOWN(null);

    private final String name;

    static VersionUpdate of(String name) {
        if (LATEST.name.equals(name)) {
            return LATEST;
        }
        if (NEXT.name.equals(name)) {
            return NEXT;
        }
        if (NONE.name.equals(name)) {
            return NONE;
        }
        return UNKNOWN;
    }

    VersionUpdate(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String toString() {
        return name;
    }
}