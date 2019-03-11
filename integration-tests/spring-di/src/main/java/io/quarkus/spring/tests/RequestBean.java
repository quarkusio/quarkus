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

package io.quarkus.spring.tests;

import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PostConstruct;

@RequestService
public class RequestBean {
    static final AtomicInteger N = new AtomicInteger();
    int n;

    @PostConstruct
    public void postConstruct() {
        this.n = N.getAndIncrement();
    }

    public int getValue() {
        return n;
    }
}
