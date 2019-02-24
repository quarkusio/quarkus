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

package io.quarkus.arc.test.interceptors;

import javax.annotation.PostConstruct;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;

@Lifecycle
@Dependent
public class SimpleBean {

    private String val;

    private Counter counter;

    @Inject
    SimpleBean(Counter counter) {
        this.counter = counter;
    }

    @PostConstruct
    void superCoolInit() {
        val = "foo";
    }

    @Logging
    @Simple
    String foo(String anotherVal) {
        return val;
    }

    String bar() {
        return new StringBuilder(val).reverse().toString();
    }

    @Simple
    void baz(Integer dummy) {
    }

    Counter getCounter() {
        return counter;
    }

}
