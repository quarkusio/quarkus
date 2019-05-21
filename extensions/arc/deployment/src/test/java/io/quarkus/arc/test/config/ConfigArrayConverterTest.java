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

package io.quarkus.arc.test.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class ConfigArrayConverterTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(Configured.class)
                    .addAsResource(new StringAsset("foos=1,2,bar\nbools=true,false"), "application.properties"));

    @Inject
    Configured configured;

    @Test
    public void testFoo() {
        assertEquals(3, configured.foos.length);
        assertEquals("1", configured.foos[0]);
        assertEquals("2", configured.foos[1]);
        assertEquals("bar", configured.foos[2]);
        // Boolean[]
        assertEquals(2, configured.bools.length);
        assertEquals(false, configured.bools[1]);
        // boolean[]
        assertEquals(2, configured.boolsPrimitives.length);
        assertEquals(true, configured.boolsPrimitives[0]);
    }

    @Singleton
    static class Configured {

        @Inject
        @ConfigProperty(name = "foos")
        String[] foos;

        @Inject
        @ConfigProperty(name = "bools_primitives", defaultValue = "true,true")
        boolean[] boolsPrimitives;

        @Inject
        @ConfigProperty(name = "bools")
        Boolean[] bools;

    }

}
