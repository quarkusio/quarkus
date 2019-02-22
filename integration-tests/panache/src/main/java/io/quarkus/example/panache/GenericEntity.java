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

package io.quarkus.example.panache;

import javax.persistence.MappedSuperclass;

import io.quarkus.panache.jpa.PanacheEntity;

@MappedSuperclass
public abstract class GenericEntity<T> extends PanacheEntity {
    public T t;
    public T t2;

    public T getT2() {
        return t2;
    }

    public void setT2(T t2) {
        this.t2 = t2;
    }
}
