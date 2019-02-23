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

import static org.wildfly.common.Assert.assertNotNull;
import static org.wildfly.common.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.scheduler.api.Scheduler;
import io.quarkus.test.QuarkusUnitTest;

public class SimpleScheduledMethodTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(SimpleJobs.class)
                    .addAsManifestResource(new StringAsset("simpleJobs.cron=0/1 * * * * ?\nsimpleJobs.every=1s"),
                            "microprofile-config.properties"));

    @Inject
    Scheduler scheduler;

    @Test
    public void testSimpleScheduledJobs() throws InterruptedException {
        for (CountDownLatch latch : SimpleJobs.LATCHES.values()) {
            Assertions.assertTrue(latch.await(4, TimeUnit.SECONDS));
        }
    }

    @Test
    public void testSchedulerTimer() throws InterruptedException {
        assertNotNull(scheduler);
        CountDownLatch latch = new CountDownLatch(1);
        scheduler.startTimer(300, () -> latch.countDown());
        assertTrue(latch.await(1, TimeUnit.SECONDS));
    }

}
