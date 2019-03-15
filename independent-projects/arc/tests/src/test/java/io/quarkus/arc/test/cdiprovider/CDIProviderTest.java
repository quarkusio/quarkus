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

package io.quarkus.arc.test.cdiprovider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import io.quarkus.arc.test.ArcTestContainer;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.spi.CDI;
import org.junit.AfterClass;
import org.junit.Rule;
import org.junit.Test;

public class CDIProviderTest {

    @Rule
    public ArcTestContainer container = new ArcTestContainer(Moo.class);

    @Test
    public void testProducer() throws IOException {
        Moo moo = CDI.current()
                .select(Moo.class)
                .get();
        assertEquals(10, moo.getVal());
    }

    @AfterClass
    public static void unset() {
        assertTrue(Moo.DESTROYED.get());
        try {
            Field providerField = CDI.class.getDeclaredField("configuredProvider");
            providerField.setAccessible(true);
            providerField.set(null, null);
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    @Dependent
    static class Moo {

        private int val;

        static final AtomicBoolean DESTROYED = new AtomicBoolean();

        @PostConstruct
        void init() {
            val = 10;
        }

        @PreDestroy
        void destroy() {
            DESTROYED.set(true);
        }

        int getVal() {
            return val;
        }

    }

}
