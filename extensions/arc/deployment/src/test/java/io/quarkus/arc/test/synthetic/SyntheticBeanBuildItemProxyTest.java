package io.quarkus.arc.test.synthetic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Consumer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Vetoed;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
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
                            Method test = recorderProxy.getClass().getDeclaredMethod("test", String.class);

                            Object proxy1 = test.invoke(recorderProxy, "ok");
                            ExtendedBeanConfigurator configurator1 = SyntheticBeanBuildItem.configure(SynthBean.class)
                                    .scope(ApplicationScoped.class)
                                    .identifier("ok")
                                    .unremovable();
                            // No creator
                            assertThrows(IllegalStateException.class,
                                    () -> configurator1.done());
                            // Not a returned proxy
                            assertThrows(IllegalArgumentException.class,
                                    () -> configurator1.runtimeProxy(new SynthBean()));
                            context.produce(configurator1
                                    .runtimeProxy(proxy1)
                                    .done());

                            // Register a synthetic bean with same types and qualifiers but different identifier
                            context.produce(SyntheticBeanBuildItem.configure(SynthBean.class)
                                    .scope(ApplicationScoped.class)
                                    .identifier("nok")
                                    .unremovable()
                                    .runtimeProxy(test.invoke(recorderProxy, "nok"))
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

        public SynthBean test(String val) {
            SynthBean bean = new SynthBean();
            bean.setValue(val);
            return bean;
        }

    }

    @Test
    public void testBeans() {
        List<InstanceHandle<SynthBean>> beans = Arc.container().listAll(SynthBean.class);
        assertEquals(2, beans.size());
        int countOk = 0;
        int countNok = 0;
        for (InstanceHandle<SynthBean> handle : beans) {
            String val = handle.get().getValue();
            if ("ok".equals(val)) {
                countOk++;
            } else if ("nok".equals(val)) {
                countNok++;
            } else {
                fail("Expected 'ok' or 'nok'");
            }
        }
        assertEquals(1, countOk);
        assertEquals(1, countNok);
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
