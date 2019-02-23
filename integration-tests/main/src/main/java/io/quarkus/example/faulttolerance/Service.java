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

package io.quarkus.example.faulttolerance;

import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Retry;

@ApplicationScoped
public class Service {

    static final int THRESHOLD = 2;

    private String name;

    @PostConstruct
    void init() {
        name = "Lucie";
    }

    @Retry(maxRetries = 10)
    public String getName(AtomicInteger counter) {
        if (counter.incrementAndGet() >= THRESHOLD) {
            return name;
        }
        throw new IllegalStateException("Counter=" + counter.get());
    }

}
