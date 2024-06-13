package io.quarkus.arc.test.startup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Vetoed;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.test.QuarkusUnitTest;

public class SyntheticBeanStartupTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(
                    root -> root.addClasses(SyntheticBeanStartupTest.class, SynthBean.class, SynthBeanCreator.class))
            .addBuildChainCustomizer(buildCustomizer());

    static Consumer<BuildChainBuilder> buildCustomizer() {
        return new Consumer<BuildChainBuilder>() {

            @Override
            public void accept(BuildChainBuilder builder) {
                builder.addBuildStep(new BuildStep() {
                    @Override
                    public void execute(BuildContext context) {
                        context.produce(SyntheticBeanBuildItem.configure(SynthBean.class)
                                .scope(ApplicationScoped.class)
                                .identifier("ok")
                                .startup()
                                .creator(SynthBeanCreator.class)
                                .done());
                    }
                }).produces(SyntheticBeanBuildItem.class).build();
            }
        };
    }

    @Inject
    Provider<SynthBean> synthBean;

    @Test
    public void testStartup() {
        assertTrue(SynthBeanCreator.CREATED.get());
        assertEquals("foo", synthBean.get().getValue());
    }

    public static class SynthBeanCreator implements BeanCreator<SynthBean> {

        static final AtomicBoolean CREATED = new AtomicBoolean();

        @Override
        public SynthBean create(SyntheticCreationalContext<SynthBean> context) {
            CREATED.set(true);
            return new SynthBean("foo");
        }
    }

    @Vetoed
    public static class SynthBean {

        private final String value;

        public SynthBean(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
