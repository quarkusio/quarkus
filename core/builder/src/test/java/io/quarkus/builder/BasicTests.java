package io.quarkus.builder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import io.quarkus.builder.item.SimpleBuildItem;

public class BasicTests {

    public static final class DummyItem extends SimpleBuildItem {
    }

    public static final class DummyItem2 extends SimpleBuildItem {
    }

    @Test
    public void testSimple() throws ChainBuildException, BuildException {
        final BuildChainBuilder builder = BuildChain.builder();
        final AtomicBoolean ran = new AtomicBoolean();
        BuildStepBuilder stepBuilder = builder.addBuildStep(new BuildStep() {
            @Override
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
    public void testFailure() throws ChainBuildException, BuildException {
        final BuildChainBuilder builder = BuildChain.builder();
        BuildStepBuilder stepBuilder = builder.addBuildStep(new BuildStep() {
            @Override
            public void execute(final BuildContext context) {
                throw new NoClassDefFoundError();
            }
        });
        stepBuilder.produces(DummyItem.class);
        stepBuilder.build();
        builder.addFinal(DummyItem.class);
        BuildChain chain = builder.build();
        Assertions.assertThrows(BuildException.class, new Executable() {
            @Override
            public void execute() throws Throwable {
                chain.createExecutionBuilder("my-app.jar").execute();
            }
        });
    }

    @Test
    public void testFailure2() throws ChainBuildException, BuildException {
        final BuildChainBuilder builder = BuildChain.builder();
        BuildStepBuilder stepBuilder = builder.addBuildStep(new BuildStep() {
            @Override
            public void execute(final BuildContext context) {
                throw new NoClassDefFoundError();
            }
        });

        final AtomicBoolean ran = new AtomicBoolean();
        stepBuilder.produces(DummyItem.class);
        stepBuilder.build();
        stepBuilder = builder.addBuildStep(new BuildStep() {
            @Override
            public void execute(final BuildContext context) {
                ran.set(true);
            }
        });
        stepBuilder.consumes(DummyItem.class);
        stepBuilder.produces(DummyItem2.class);
        stepBuilder.build();
        builder.addFinal(DummyItem2.class);
        BuildChain chain = builder.build();
        Assertions.assertFalse(ran.get());
        Assertions.assertThrows(BuildException.class, new Executable() {
            @Override
            public void execute() throws Throwable {
                chain.createExecutionBuilder("my-app.jar").execute();
            }
        });
    }

    @Test
    public void testLinked() throws ChainBuildException, BuildException {
        final BuildChainBuilder builder = BuildChain.builder();
        BuildStepBuilder stepBuilder = builder.addBuildStep(new BuildStep() {
            @Override
            public void execute(final BuildContext context) {
                context.produce(new DummyItem());
            }
        });
        stepBuilder.produces(DummyItem.class);
        stepBuilder.build();
        stepBuilder = builder.addBuildStep(new BuildStep() {
            @Override
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
            @Override
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
            @Override
            public void execute(final BuildContext context) {
                context.produce(new DummyItem());
            }
        });
        stepBuilder.produces(DummyItem.class);
        stepBuilder.build();
        final AtomicBoolean ran = new AtomicBoolean();
        stepBuilder = builder.addBuildStep(new BuildStep() {
            @Override
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
    public void testMissingProduces() {
        final BuildChainBuilder builder = BuildChain.builder();
        BuildStepBuilder stepBuilder = builder.addBuildStep(new BuildStep() {
            @Override
            public void execute(final BuildContext context) {
                context.produce(new DummyItem());
            }

            @Override
            public String getId() {
                return "myBuildStepId";
            }
        });
        stepBuilder.consumes(DummyItem.class);
        assertThatThrownBy(stepBuilder::build)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContainingAll(
                        "Build step 'myBuildStepId'",
                        "does not produce any build item and thus will never get executed",
                        "change the return type of the method to a build item type",
                        "add a parameter of type BuildProducer<[some build item type]>/Consumer<[some build item type]>",
                        "annotate the method with @Produces",
                        "Use @Produce(ArtifactResultBuildItem.class) if you want to always execute this step");
    }

    @Test
    public void testCircular() {
        final BuildChainBuilder builder = BuildChain.builder();
        builder.addFinal(DummyItem.class);
        BuildStepBuilder stepBuilder = builder.addBuildStep(new BuildStep() {
            @Override
            public void execute(final BuildContext context) {
                context.consume(DummyItem2.class);
                context.produce(new DummyItem());
            }
        });
        stepBuilder.produces(DummyItem.class);
        stepBuilder.consumes(DummyItem2.class);
        stepBuilder.build();
        stepBuilder = builder.addBuildStep(new BuildStep() {
            @Override
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

    @Test
    public void testDuplicate() {
        final BuildChainBuilder builder = BuildChain.builder();
        builder.addFinal(DummyItem.class);
        BuildStepBuilder stepBuilder = builder.addBuildStep(new BuildStep() {
            @Override
            public void execute(final BuildContext context) {
                context.produce(new DummyItem());
            }
        });
        stepBuilder.produces(DummyItem.class);
        stepBuilder.build();
        stepBuilder = builder.addBuildStep(new BuildStep() {
            @Override
            public void execute(final BuildContext context) {
                context.produce(new DummyItem());
            }
        });
        stepBuilder.produces(DummyItem.class);
        stepBuilder.build();
        try {
            builder.build();
            fail("Expected exception");
        } catch (ChainBuildException expected) {
            // ok
            assertFalse(expected.getMessage().contains("overridable"));
        }
    }

    @Test
    public void testDuplicateOverridable() {
        final BuildChainBuilder builder = BuildChain.builder();
        builder.addFinal(DummyItem.class);
        BuildStepBuilder stepBuilder = builder.addBuildStep(new BuildStep() {
            @Override
            public void execute(final BuildContext context) {
                context.produce(new DummyItem());
            }
        });
        stepBuilder.produces(DummyItem.class, ProduceFlag.OVERRIDABLE);
        stepBuilder.build();
        stepBuilder = builder.addBuildStep(new BuildStep() {
            @Override
            public void execute(final BuildContext context) {
                context.produce(new DummyItem());
            }
        });
        stepBuilder.produces(DummyItem.class, ProduceFlag.OVERRIDABLE);
        stepBuilder.build();
        try {
            builder.build();
            fail("Expected exception");
        } catch (ChainBuildException expected) {
            // ok
            assertTrue(expected.getMessage().contains("overridable"));
        }
    }

    @Test
    public void testOverride() throws ChainBuildException {
        final BuildChainBuilder builder = BuildChain.builder();
        builder.addFinal(DummyItem.class);
        BuildStepBuilder stepBuilder = builder.addBuildStep(new BuildStep() {
            @Override
            public void execute(final BuildContext context) {
                context.produce(new DummyItem());
            }
        });
        stepBuilder.produces(DummyItem.class, ProduceFlag.OVERRIDABLE);
        stepBuilder.build();
        stepBuilder = builder.addBuildStep(new BuildStep() {
            @Override
            public void execute(final BuildContext context) {
                context.produce(new DummyItem());
            }
        });
        stepBuilder.produces(DummyItem.class);
        stepBuilder.build();
        builder.build();
    }
}
