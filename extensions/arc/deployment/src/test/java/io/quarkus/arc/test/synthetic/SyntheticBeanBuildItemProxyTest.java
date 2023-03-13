package io.quarkus.arc.test.synthetic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Method;
import java.util.function.Consumer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Vetoed;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem.ExtendedBeanConfigurator;
import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.deployment.builditem.StaticBytecodeRecorderBuildItem;
import io.quarkus.deployment.recording.BytecodeRecorderImpl;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.test.QuarkusUnitTest;

public class SyntheticBeanBuildItemProxyTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(root -> root.addClasses(SyntheticBeanBuildItemProxyTest.class, SynthBean.class))
            .addBuildChainCustomizer(buildCustomizer());

    static Consumer<BuildChainBuilder> buildCustomizer() {
        return new Consumer<BuildChainBuilder>() {

            @Override
            public void accept(BuildChainBuilder builder) {
                builder.addBuildStep(new BuildStep() {

                    @Override
                    public void execute(BuildContext context) {
                        BytecodeRecorderImpl bytecodeRecorder = new BytecodeRecorderImpl(true,
                                TestRecorder.class.getSimpleName(),
                                "test", "" + TestRecorder.class.hashCode(), true, s -> null);
                        // We need to use reflection due to some class loading problems
                        Object recorderProxy = bytecodeRecorder.getRecordingProxy(TestRecorder.class);
                        try {
                            Method test = recorderProxy.getClass().getDeclaredMethod("test");
                            Object proxy = test.invoke(recorderProxy);
                            ExtendedBeanConfigurator configurator = SyntheticBeanBuildItem.configure(SynthBean.class)
                                    .scope(ApplicationScoped.class)
                                    .unremovable();
                            // No creator
                            assertThrows(IllegalStateException.class,
                                    () -> configurator.done());
                            // Not a returned proxy
                            assertThrows(IllegalArgumentException.class,
                                    () -> configurator.runtimeProxy(new SynthBean()));
                            context.produce(configurator
                                    .runtimeProxy(proxy)
                                    .done());
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        context.produce(new StaticBytecodeRecorderBuildItem(bytecodeRecorder));
                    }
                }).produces(StaticBytecodeRecorderBuildItem.class).produces(SyntheticBeanBuildItem.class).build();
            }
        };
    }

    @Recorder
    public static class TestRecorder {

        public SynthBean test() {
            SynthBean bean = new SynthBean();
            bean.setValue("ok");
            return bean;
        }

    }

    @Test
    public void testBeans() {
        SynthBean bean = Arc.container().instance(SynthBean.class).get();
        assertNotNull(bean);
        assertEquals("ok", bean.getValue());
    }

    @Vetoed
    public static class SynthBean {

        private String value;

        public SynthBean() {
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

    }
}
