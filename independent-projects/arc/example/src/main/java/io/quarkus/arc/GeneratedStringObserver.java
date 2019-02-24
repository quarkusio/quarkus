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

import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class GeneratedStringObserver {

    @Inject
    @ConfigProperty(name = "logEvery", defaultValue = "1000")
    Integer logEvery;

    private final AtomicInteger COUNT = new AtomicInteger(0);

    void stringGenerated(@Observes @Generated String generated) {
        if (COUNT.incrementAndGet() % logEvery == 0) {
            System.out.println("Generated " + COUNT.get() + " strings");
        }
    }

}
