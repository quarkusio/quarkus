/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.renov8.resolver.test;

import static org.junit.Assert.assertEquals;

import org.jboss.renov8.PackLocation;
import org.jboss.renov8.test.Renov8TestBase;
import org.jboss.renov8.test.StrVersion;
import org.jboss.renov8.test.TestPack;
import org.junit.Test;

/**
 *
 * @author Alexey Loubyansky
 */
public class BasicResolveHighestVersionTest extends Renov8TestBase {

    @Override
    protected void createPacks() throws Exception {
        createPack(TestPack.builder(PackLocation.create("A", new StrVersion("1"))).build());
        createPack(TestPack.builder(PackLocation.create("A", new StrVersion("3"))).build());
        createPack(TestPack.builder(PackLocation.create("A", new StrVersion("2"))).build());

        createPack(TestPack.builder(PackLocation.create("B", new StrVersion("2"))).build());

        createPack(TestPack.builder(PackLocation.create("C", new StrVersion("1"))).build());
        createPack(TestPack.builder(PackLocation.create("C", new StrVersion("10"))).build());
    }

    @Test
    public void test() throws Exception {
        assertEquals(new StrVersion("3"), packResolver.getLatestVersion(PackLocation.create("A", new StrVersion("2"))));
        assertEquals(new StrVersion("2"), packResolver.getLatestVersion(PackLocation.create("B", new StrVersion("2"))));
        assertEquals(new StrVersion("10"), packResolver.getLatestVersion(PackLocation.create("C", new StrVersion("1"))));
    }
}
