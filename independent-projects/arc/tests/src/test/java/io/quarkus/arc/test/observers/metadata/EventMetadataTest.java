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

package io.quarkus.arc.test.observers.metadata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.spi.EventMetadata;
import javax.inject.Singleton;
import org.junit.Rule;
import org.junit.Test;

public class EventMetadataTest {

    @Rule
    public ArcTestContainer container = new ArcTestContainer(BigDecimalObserver.class);

    @Test
    public void testMetadata() {
        Arc.container().beanManager().getEvent().fire(BigDecimal.ONE);
        EventMetadata metadata = BigDecimalObserver.METADATA.get();
        assertNotNull(metadata);
        assertEquals(1, metadata.getQualifiers().size());
        assertEquals(Any.class, metadata.getQualifiers().iterator().next().annotationType());
        assertEquals(BigDecimal.class, metadata.getType());
    }

    @Singleton
    static class BigDecimalObserver {

        static final AtomicReference<EventMetadata> METADATA = new AtomicReference<EventMetadata>();

        void observe(@Observes BigDecimal value, EventMetadata metadata) {
            METADATA.set(metadata);
        }

    }

}
