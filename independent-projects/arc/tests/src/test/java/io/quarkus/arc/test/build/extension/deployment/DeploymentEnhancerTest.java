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

package io.quarkus.arc.test.build.extension.deployment;

import static org.junit.Assert.assertTrue;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import javax.enterprise.context.Dependent;
import org.junit.Rule;
import org.junit.Test;

public class DeploymentEnhancerTest {

    @Rule
    public ArcTestContainer container = ArcTestContainer.builder().deploymentEnhancers(dc -> dc.addClass(Fool.class)).build();

    @Test
    public void testEnhancer() {
        assertTrue(Arc.container().instance(Fool.class).isAvailable());
    }

    // => this class is not part of the original deployment
    @Dependent
    static class Fool {

    }

}
