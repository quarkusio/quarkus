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

package io.quarkus.arc;

import java.util.function.Supplier;

/**
 *
 * @author Martin Kouba
 */
public class LazyValue<T> {

    private final Supplier<T> supplier;

    private transient volatile T value;

    public LazyValue(Supplier<T> supplier) {
        this.supplier = supplier;
    }

    public T get() {
        T valueCopy = value;
        if (valueCopy != null) {
            return valueCopy;
        }
        synchronized (this) {
            if (value == null) {
                value = supplier.get();
            }
            return value;
        }
    }

    public T getIfPresent() {
        return value;
    }

    public void clear() {
        synchronized (this) {
            value = null;
        }
    }

    public boolean isSet() {
        return value != null;
    }

}
