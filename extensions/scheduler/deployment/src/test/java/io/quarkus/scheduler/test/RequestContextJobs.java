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
package io.quarkus.scheduler.test;

import java.util.concurrent.CountDownLatch;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import io.quarkus.scheduler.Scheduled;

public class RequestContextJobs {

    static final CountDownLatch LATCH = new CountDownLatch(1);

    @Inject
    RequestFoo foo;

    @Scheduled(every = "1s")
    void checkEverySecond() {
        foo.getName();
        LATCH.countDown();
    }

    @RequestScoped
    static class RequestFoo {

        private String name;

        @PostConstruct
        void init() {
            name = "oof";
        }

        public String getName() {
            return name;
        }

    }

}
