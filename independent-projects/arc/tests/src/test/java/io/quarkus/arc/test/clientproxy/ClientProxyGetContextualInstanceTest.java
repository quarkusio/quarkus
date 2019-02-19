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

package org.jboss.quarkus.arc.test.clientproxy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

import org.jboss.quarkus.arc.Arc;
import org.jboss.quarkus.arc.ClientProxy;
import org.jboss.quarkus.arc.test.ArcTestContainer;
import org.junit.Rule;
import org.junit.Test;

public class ClientProxyGetContextualInstanceTest {

    @Rule
    public ArcTestContainer container = new ArcTestContainer(Moo.class);

    @Test
    public void testProducer() throws IOException {
        Moo moo = Arc.container().instance(Moo.class).get();
        assertTrue(moo instanceof ClientProxy);
        assertEquals(10, ((Moo)((ClientProxy)moo).getContextualInstance()).val);
    }

    @ApplicationScoped
    static class Moo {

        private int val;
        
        @PostConstruct
        void init() {
            val = 10;
        }

    }

}
