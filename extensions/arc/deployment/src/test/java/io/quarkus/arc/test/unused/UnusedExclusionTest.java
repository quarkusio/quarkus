package io.quarkus.arc.test.unused;

import java.lang.reflect.Method;
import java.util.function.Consumer;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.arc.test.unused.subpackage.Beta;
import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.deployment.builditem.StaticBytecodeRecorderBuildItem;
import io.quarkus.deployment.recording.BytecodeRecorderImpl;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.test.ExpectLogMessage;
import io.quarkus.test.QuarkusUnitTest;

@ExpectLogMessage("programmatic lookup problem detected")
public class UnusedExclusionTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(UnusedExclusionTest.class, Alpha.class, Beta.class, Charlie.class, Delta.class,
                            ProducerBean.class, TestRecorder.class, Gama.class, GamaProducer.class)
                    .addAsResource(new StringAsset(
                            "quarkus.arc.unremovable-types=io.quarkus.arc.test.unused.UnusedExclusionTest$Alpha,io.quarkus.arc.test.unused.subpackage.**,io.quarkus.arc.test.unused.Charlie,Delta"),
                            "application.properties"))
            .addBuildChainCustomizer(buildCustomizer());

    static Consumer<BuildChainBuilder> buildCustomizer() {
        return new Consumer<BuildChainBuilder>() {

            @Override
            public void accept(BuildChainBuilder builder) {
                builder.addBuildStep(new BuildStep() {

                    @Override
                    public void execute(BuildContext context) {
                        BeanContainer beanContainer = context.consume(BeanContainerBuildItem.class).getValue();
                        BytecodeRecorderImpl bytecodeRecorder = new BytecodeRecorderImpl(true,
                                TestRecorder.class.getSimpleName(),
                                "test", "" + TestRecorder.class.hashCode());
                        // We need to use reflection due to some class loading problems
                        Object recorderProxy = bytecodeRecorder.getRecordingProxy(TestRecorder.class);
                        try {
                            Method test = recorderProxy.getClass().getDeclaredMethod("test", BeanContainer.class);
                            Object[] args = new Object[1];
                            args[0] = beanContainer;
                            test.invoke(recorderProxy, args);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        context.produce(new StaticBytecodeRecorderBuildItem(bytecodeRecorder));
                    }
                }).consumes(BeanContainerBuildItem.class).produces(StaticBytecodeRecorderBuildItem.class).build();
            }
        };
    }

    @Recorder
    public static class TestRecorder {

        public void test(BeanContainer beanContainer) {
            // This should trigger the warning - Gama was removed
            Gama gama = beanContainer.instance(Gama.class);
            // Test that fallback was used - no injection was performed
            Assertions.assertNull(gama.beanManager);
        }

    }

    @Test
    public void testBeans() {
        ArcContainer container = Arc.container();
        String expectedBeanResponse = "ok";
        InstanceHandle<Alpha> alphaInstance = container.instance(Alpha.class);
        Assertions.assertTrue(alphaInstance.isAvailable());
        Assertions.assertEquals(expectedBeanResponse, alphaInstance.get().ping());

        InstanceHandle<Beta> betaInstance = container.instance(Beta.class);
        Assertions.assertTrue(betaInstance.isAvailable());
        Assertions.assertEquals(expectedBeanResponse, betaInstance.get().ping());

        InstanceHandle<Charlie> charlieInstance = container.instance(Charlie.class);
        Assertions.assertTrue(charlieInstance.isAvailable());
        Assertions.assertEquals(expectedBeanResponse, charlieInstance.get().ping());

        InstanceHandle<Delta> deltaInstance = container.instance(Delta.class);
        Assertions.assertTrue(deltaInstance.isAvailable());
        Assertions.assertEquals(expectedBeanResponse, deltaInstance.get().ping());

        Assertions.assertFalse(container.instance(Gama.class).isAvailable());
    }

    // unused bean, won't be removed
    @ApplicationScoped
    static class Alpha {

        public String ping() {
            return "ok";
        }

    }

    // unused bean, will be removed
    @ApplicationScoped
    public static class Gama {

        @Inject
        BeanManager beanManager;

        public String ping() {
            return "ok";
        }

    }

    public static class GamaProducer {

        @Produces
        public Gama ping() {
            return new Gama();
        }

    }
}
