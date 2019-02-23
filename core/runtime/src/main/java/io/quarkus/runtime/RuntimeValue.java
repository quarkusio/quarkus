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

package io.quarkus.runtime;

/**
 * Represents a proxyable object that can be returned from a bytecode recorder,
 * and passed between recorders.
 *
 */
public class RuntimeValue<T> {

    private final T value;

    public RuntimeValue(T value) {
        this.value = value;
    }

    public RuntimeValue() {
        this.value = null;
    }

    public T getValue() {
        if (value == null) {
            throw new IllegalStateException("Cannot call getValue() at deployment time");
        }
        return value;
    }
}
