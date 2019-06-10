/*
 * Copyright 2019 Red Hat, Inc.
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

package io.quarkus.arc.test.clientproxy.constructor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ClientProxy;
import io.quarkus.arc.test.ArcTestContainer;
import java.io.IOException;
import javax.enterprise.context.ApplicationScoped;
import org.junit.Rule;
import org.junit.Test;

public class ClientProxyConstructorGuardTest {

    @Rule
    public ArcTestContainer container = new ArcTestContainer(Moo.class);

    @Test
    public void testProducer() throws IOException {
        Moo moo = Arc.container().instance(Moo.class).get();
        assertTrue(moo instanceof ClientProxy);
        assertEquals(10, ((Moo) ((ClientProxy) moo).getContextualInstance()).val);
    }

    @ApplicationScoped
    static class Moo {

        private int val;

        public Moo() {
            init();
        }

        void init() {
            val = 10;
        }

    }

}
