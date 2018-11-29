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

package org.jboss.renov8.config.resolved.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;

import org.jboss.renov8.config.DistConfig;
import org.jboss.renov8.resolver.DistResolver;
import org.jboss.renov8.test.Renov8TestBase;
import org.jboss.renov8.test.TestPack;
import org.junit.Test;

/**
 *
 * @author Alexey Loubyansky
 */
public abstract class ResolvedSpecTestBase extends Renov8TestBase {

    @Test
    public void test() throws Exception {
        final String[] errors = errors();
        final TestPack[] expected = resolvedPacks();
        try {
            final String[] resolveLatest = updateProducers();
            final List<TestPack> actual;
            final DistConfig originalConfig = distConfig();

            final DistResolver<TestPack> resolver = resolveLatest == null ?
                    DistResolver.newInstance(packResolver, originalConfig) :
                        DistResolver.newInstance(packResolver, originalConfig, resolveLatest);

            actual = resolver.getPacksInOrder();

            if(errors != null) {
                fail("Expected failures: " + Arrays.asList(errors));
            }
            if(!Arrays.asList(expected).equals(actual)) {
                System.err.println(Arrays.asList(expected));
                System.err.println(Arrays.asList(actual));
                assertEquals(Arrays.asList(expected), actual);
            }
            DistConfig expectedConfig = resolvedConfig();
            if(expectedConfig == null) {
                expectedConfig = originalConfig;
            }
            assertEquals(expectedConfig, resolver.getConfig());
        } catch(AssertionError e) {
            throw e;
        } catch(Throwable t) {
            if (errors == null) {
                throw t;
            } else {
                assertErrors(t, errors);
            }
            if(expected != null) {
                fail("Expected install " + expected);
            }
        }
    }

    protected String[] updateProducers() {
        return null;
    }

    protected String[] errors() {
        return null;
    }

    protected abstract DistConfig distConfig();

    protected DistConfig resolvedConfig() {
        return null;
    }

    protected TestPack[] resolvedPacks() {
        return null;
    }

    protected void assertErrors(Throwable t, String... msgs) {
        int i = 0;
        if(msgs != null) {
            while (t != null && i < msgs.length) {
                assertEquals(msgs[i++], t.getLocalizedMessage());
                t = t.getCause();
            }
        }
        if(t != null) {
            fail("Unexpected error: " + t.getLocalizedMessage());
        }
        if(i < msgs.length - 1) {
            fail("Not reported error: " + msgs[i]);
        }
    }
}
