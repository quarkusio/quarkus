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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import io.quarkus.scheduler.api.Scheduled;

public class SimpleJobs {

    static final Map<String, CountDownLatch> LATCHES;

    static {
        LATCHES = new ConcurrentHashMap<>();
        LATCHES.put("every", new CountDownLatch(2));
        LATCHES.put("everyConfig", new CountDownLatch(2));
        LATCHES.put("cron", new CountDownLatch(2));
        LATCHES.put("cronConfig", new CountDownLatch(2));
    }

    @Scheduled(cron = "0/1 * * * * ?")
    void checkEverySecondCron() {
        LATCHES.get("cron").countDown();
    }

    @Scheduled(every = "1s")
    void checkEverySecond() {
        LATCHES.get("every").countDown();
    }

    @Scheduled(cron = "{simpleJobs.cron}")
    void checkEverySecondCronConfig() {
        LATCHES.get("cronConfig").countDown();
    }

    @Scheduled(every = "{simpleJobs.every}")
    void checkEverySecondConfig() {
        LATCHES.get("everyConfig").countDown();
    }

}
