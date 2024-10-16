package io.quarkus.arc.test.unused;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.Consumer;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Vetoed;

import org.jboss.jandex.ClassType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.test.QuarkusUnitTest;

public class UnremovableSyntheticInjectionPointTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(UnremovableSyntheticInjectionPointTest.class, Alpha.class, Gama.class, GamaCreator.class))
            .addBuildChainCustomizer(buildCustomizer());

    static Consumer<BuildChainBuilder> buildCustomizer() {
        return new Consumer<BuildChainBuilder>() {

            @Override
            public void accept(BuildChainBuilder builder) {
                builder.addBuildStep(new BuildStep() {

                    @Override
                    public void execute(BuildContext context) {
                        context.produce(SyntheticBeanBuildItem.configure(Gama.class)
                                .scope(BuiltinScope.SINGLETON.getInfo())
                                .unremovable()
                                .addInjectionPoint(ClassType.create(Alpha.class))
                                .creator(GamaCreator.class)
                                .done());
                    }
                }).produces(SyntheticBeanBuildItem.class).build();
            }
        };
    }

    @Test
    public void testBeans() {
        ArcContainer container = Arc.container();
        InstanceHandle<Alpha> alpha = container.instance(Alpha.class);
        assertTrue(alpha.isAvailable());
        assertTrue(alpha.get().ping());
        InstanceHandle<Gama> gama = container.instance(Gama.class);
        assertTrue(gama.isAvailable());
        assertTrue(gama.get().ping());
    }

    // unused bean injected into a synthetic injection point
    @ApplicationScoped
    public static class Alpha {

        volatile boolean flag;

        @PostConstruct
        void init() {
            flag = true;
        }

        public boolean ping() {
            return flag;
        }

    }

    @Vetoed
    public static class Gama {

        private final Alpha alpha;

        private Gama(Alpha alpha) {
            this.alpha = alpha;
        }

        public boolean ping() {
            return alpha.ping();
        }

    }

    public static class GamaCreator implements BeanCreator<Gama> {

        @Override
        public Gama create(SyntheticCreationalContext<Gama> context) {
            return new Gama(context.getInjectedReference(Alpha.class));
        }

    }

}
