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

package io.quarkus.arc.test.producer.async;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import org.junit.Rule;
import org.junit.Test;

public class AsyncProducerTest {

    @Rule
    public ArcTestContainer container = new ArcTestContainer(LongProducer.class, LongClient.class);

    @Test
    public void testAsyncProducer() throws InterruptedException, ExecutionException {
        LongProducer.reset();
        LongClient longClient = Arc.container().instance(LongClient.class).get();
        AtomicReference<Long> val = new AtomicReference<>();
        longClient.completionStage.thenAccept(l -> {
            val.set(l);
        });

        assertNull(val.get());

        LongProducer.complete(10);

        assertEquals(Long.valueOf(10), val.get());
        assertEquals(Long.valueOf(10), longClient.completionStage.toCompletableFuture().get());
    }

    @Dependent
    static class LongClient {

        @Inject
        CompletionStage<Long> completionStage;

    }

    @Singleton
    static class LongProducer {

        private static CompletableFuture<Long> future;

        static void complete(long code) {
            future.complete(code);
        }

        static void reset() {
            future = new CompletableFuture<>();
        }

        @Produces
        CompletionStage<Long> produceLong() {
            return future;
        }

    }

}
