/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
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

package org.jboss.builder;

import static org.junit.Assert.*;

import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.builder.item.SimpleBuildItem;
import org.junit.Test;

/**
 */
public class BasicTests {

    public static final class DummyItem extends SimpleBuildItem {}

    public static final class DummyItem2 extends SimpleBuildItem {}

    @Test
    public void testSimple() throws ChainBuildException, BuildException {
        final BuildChainBuilder builder = BuildChain.builder();
        final AtomicBoolean ran = new AtomicBoolean();
        BuildStepBuilder stepBuilder = builder.addBuildStep(new BuildStep() {
            public void execute(final BuildContext context) {
                ran.set(true);
                context.produce(new DummyItem());
            }
        });
        stepBuilder.produces(DummyItem.class);
        stepBuilder.build();
        builder.addFinal(DummyItem.class);
        BuildChain chain = builder.build();
        final BuildResult result = chain.createExecutionBuilder("my-app.jar").execute();
        assertTrue(ran.get());
        assertNotNull(result.consume(DummyItem.class));
    }

    @Test
    public void testLinked() throws ChainBuildException, BuildException {
        final BuildChainBuilder builder = BuildChain.builder();
        BuildStepBuilder stepBuilder = builder.addBuildStep(new BuildStep() {
            public void execute(final BuildContext context) {
                context.produce(new DummyItem());
            }
        });
        stepBuilder.produces(DummyItem.class);
        stepBuilder.build();
        stepBuilder = builder.addBuildStep(new BuildStep() {
            public void execute(final BuildContext context) {
                assertNotNull(context.consume(DummyItem.class));
                context.produce(new DummyItem2());
            }
        });
        stepBuilder.consumes(DummyItem.class);
        stepBuilder.produces(DummyItem2.class);
        stepBuilder.build();
        builder.addFinal(DummyItem2.class);
        final BuildChain chain = builder.build();
        final BuildResult result = chain.createExecutionBuilder("my-app.jar").execute();
        assertNotNull(result.consume(DummyItem2.class));
    }

    @Test
    public void testInitial() throws ChainBuildException, BuildException {
        final BuildChainBuilder builder = BuildChain.builder();
        builder.addInitial(DummyItem.class);
        BuildStepBuilder stepBuilder = builder.addBuildStep(new BuildStep() {
            public void execute(final BuildContext context) {
                assertNotNull(context.consume(DummyItem.class));
                context.produce(new DummyItem2());
            }
        });
        stepBuilder.consumes(DummyItem.class);
        stepBuilder.produces(DummyItem2.class);
        stepBuilder.build();
        builder.addFinal(DummyItem2.class);
        final BuildChain chain = builder.build();
        final BuildExecutionBuilder eb = chain.createExecutionBuilder("my-app.jar");
        eb.produce(DummyItem.class, new DummyItem());
        final BuildResult result = eb.execute();
        assertNotNull(result.consume(DummyItem2.class));
    }

    @Test
    public void testPruning() throws ChainBuildException, BuildException {
        final BuildChainBuilder builder = BuildChain.builder();
        BuildStepBuilder stepBuilder = builder.addBuildStep(new BuildStep() {
            public void execute(final BuildContext context) {
                context.produce(new DummyItem());
            }
        });
        stepBuilder.produces(DummyItem.class);
        stepBuilder.build();
        final AtomicBoolean ran = new AtomicBoolean();
        stepBuilder = builder.addBuildStep(new BuildStep() {
            public void execute(final BuildContext context) {
                assertNotNull(context.consume(DummyItem.class));
                context.produce(new DummyItem2());
                ran.set(true);
            }
        });
        stepBuilder.consumes(DummyItem.class);
        stepBuilder.produces(DummyItem2.class);
        stepBuilder.build();
        builder.addFinal(DummyItem.class);
        final BuildChain chain = builder.build();
        final BuildResult result = chain.createExecutionBuilder("my-app.jar").execute();
        assertNotNull(result.consume(DummyItem.class));
        assertFalse(ran.get());
    }

    @Test
    public void testCircular() {
        final BuildChainBuilder builder = BuildChain.builder();
        builder.addFinal(DummyItem.class);
        BuildStepBuilder stepBuilder = builder.addBuildStep(new BuildStep() {
            public void execute(final BuildContext context) {
                context.consume(DummyItem2.class);
                context.produce(new DummyItem());
            }
        });
        stepBuilder.produces(DummyItem.class);
        stepBuilder.consumes(DummyItem2.class);
        stepBuilder.build();
        stepBuilder = builder.addBuildStep(new BuildStep() {
            public void execute(final BuildContext context) {
                context.consume(DummyItem.class);
                context.produce(new DummyItem2());
            }
        });
        stepBuilder.produces(DummyItem2.class);
        stepBuilder.consumes(DummyItem.class);
        stepBuilder.build();
        try {
            builder.build();
            fail("Expected exception");
        } catch (ChainBuildException expected) {
            // ok
        }
    }

}
