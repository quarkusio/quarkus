package io.quarkus.arc.test.synthetic;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.function.Consumer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Vetoed;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.core.deployment.action.impl.TransliteratedAction;
import io.quarkus.deployment.builditem.StaticBytecodeRecorderBuildItem;
import io.quarkus.deployment.recording.BytecodeRecorderImpl;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Tests the {@link SyntheticBeanBuildItem.ExtendedBeanConfigurator#serviceValue(Class)} API,
 * which bridges a service graph value into a CDI synthetic bean.
 * <p>
 * The test manually populates a service value in the startup context using a
 * {@link BytecodeRecorderImpl} and an {@link TransliteratedAction.AliasService},
 * then verifies that a {@code SyntheticBeanBuildItem} with {@code serviceValue()}
 * produces a CDI bean whose instance is the service value.
 */
public class SyntheticBeanServiceValueTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root.addClasses(SyntheticBeanServiceValueTest.class, SynthBean.class))
            .addBuildChainCustomizer(buildCustomizer());

    static Consumer<BuildChainBuilder> buildCustomizer() {
        return new Consumer<BuildChainBuilder>() {

            @Override
            public void accept(BuildChainBuilder builder) {
                builder.addBuildStep(new BuildStep() {

                    @Override
                    public void execute(BuildContext context) {
                        // Use a BytecodeRecorderImpl to produce a SynthBean via a @Recorder method.
                        // This stores the value in StartupContext under a recorder proxy key.
                        BytecodeRecorderImpl bytecodeRecorder = new BytecodeRecorderImpl(true,
                                TestRecorder.class.getSimpleName(),
                                "test", "" + TestRecorder.class.hashCode(), true);
                        // Reflection is needed due to classloading constraints in the test framework
                        Object recorderProxy = bytecodeRecorder.getRecordingProxy(TestRecorder.class);
                        try {
                            Method create = recorderProxy.getClass().getDeclaredMethod("create", String.class);
                            Object proxy = create.invoke(recorderProxy, "service-value");

                            // Get the auto-assigned recorder proxy key
                            String recorderKey = ((BytecodeRecorderImpl.ReturnedProxy) proxy)
                                    .__returned$proxy$key();

                            // Alias the recorder value to the service key so it appears
                            // as a service graph value in StartupContext
                            String serviceKey = SynthBean.class.getName() + ":";
                            TransliteratedAction alias = new TransliteratedAction.AliasService(
                                    serviceKey, true, recorderKey, context.getStepId());

                            // Produce items in order: recorder bytecode first, then alias
                            context.produce(new StaticBytecodeRecorderBuildItem(bytecodeRecorder, context.getStepId()));
                            context.produce(new StaticBytecodeRecorderBuildItem(alias));

                            // Produce the synthetic bean backed by the service value
                            context.produce(SyntheticBeanBuildItem.configure(SynthBean.class)
                                    .scope(ApplicationScoped.class)
                                    .serviceValue(SynthBean.class)
                                    .unremovable()
                                    .done());
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                }).produces(StaticBytecodeRecorderBuildItem.class).produces(SyntheticBeanBuildItem.class).build();
            }
        };
    }

    /**
     * Recorder that produces {@link SynthBean} instances during static init.
     */
    @Recorder
    public static class TestRecorder {

        /**
         * Create a new {@link SynthBean} with the given value.
         *
         * @param val the value to set
         * @return a new {@code SynthBean}
         */
        public SynthBean create(String val) {
            SynthBean bean = new SynthBean();
            bean.setValue(val);
            return bean;
        }
    }

    @Test
    public void testServiceValueBean() {
        InstanceHandle<SynthBean> handle = Arc.container().instance(SynthBean.class);
        assertThat(handle.isAvailable()).isTrue();
        assertThat(handle.get().getValue()).isEqualTo("service-value");
    }

    /**
     * A simple bean class used as the synthetic bean type.
     */
    @Vetoed
    public static class SynthBean {

        private String value;

        /**
         * Construct a new instance.
         */
        public SynthBean() {
        }

        /**
         * Get the value.
         *
         * @return the value
         */
        public String getValue() {
            return value;
        }

        /**
         * Set the value.
         *
         * @param value the value
         */
        public void setValue(String value) {
            this.value = value;
        }
    }
}
