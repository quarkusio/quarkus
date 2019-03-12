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

package io.quarkus.arc.test.requestcontext.propagation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collection;

import javax.enterprise.context.ContextNotActiveException;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.ContextInstanceHandle;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.ManagedContext;
import io.quarkus.arc.test.ArcTestContainer;
import org.junit.Rule;
import org.junit.Test;

public class RequestContextPropagationTest {

    @Rule
    public ArcTestContainer container = new ArcTestContainer(SuperController.class, SuperButton.class);

    @Test
    public void testPropagation() {

        ArcContainer arc = Arc.container();
        ManagedContext requestContext = arc.requestContext();

        try {
            arc.instance(SuperController.class).get().getId();
            fail();
        } catch (ContextNotActiveException expected) {
        }

        requestContext.activate();
        assertFalse(SuperController.DESTROYED.get());
        SuperController controller1 = arc.instance(SuperController.class).get();
        SuperController controller2 = arc.instance(SuperController.class).get();
        String controller2Id = controller2.getId();
        assertEquals(controller1.getId(), controller2Id);
        assertNotNull(controller2.getButton());
        assertTrue(controller2.getButton() == controller1.getButton());

        // Store existing instances
        Collection<ContextInstanceHandle<?>> instances = requestContext.getAll();
        // Deactivate but don't destroy
        requestContext.deactivate();

        assertFalse(SuperController.DESTROYED.get());
        assertFalse(SuperButton.DESTROYED.get());

        try {
            // Proxy should not work
            controller1.getId();
            fail();
        } catch (ContextNotActiveException expected) {
        }

        requestContext.activate(instances);
        assertEquals(arc.instance(SuperController.class).get().getId(), controller2Id);

        requestContext.terminate();
        assertTrue(SuperController.DESTROYED.get());
        assertTrue(SuperButton.DESTROYED.get());
    }

}
