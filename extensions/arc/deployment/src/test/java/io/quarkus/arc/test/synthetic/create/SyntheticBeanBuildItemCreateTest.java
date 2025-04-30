package io.quarkus.arc.test.synthetic.create;

import java.util.function.Consumer;

import jakarta.enterprise.inject.Vetoed;

import org.jboss.jandex.DotName;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Tests that {@link SyntheticBeanBuildItem#create(DotName)} does not add automatically register the param type as bean type
 */
public class SyntheticBeanBuildItemCreateTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(SyntheticBeanBuildItemCreateTest.class, FooCreator.class, FooInterface.class, Foo.class))
            .addBuildChainCustomizer(buildCustomizer());

    static Consumer<BuildChainBuilder> buildCustomizer() {
        return new Consumer<BuildChainBuilder>() {

            @Override
            public void accept(BuildChainBuilder builder) {
                builder.addBuildStep(new BuildStep() {

                    @Override
                    public void execute(BuildContext context) {
                        context.produce(SyntheticBeanBuildItem.create(Foo.class)
                                .addType(FooInterface.class)
                                .scope(BuiltinScope.SINGLETON.getInfo())
                                .unremovable()
                                .creator(FooCreator.class)
                                .done());
                    }
                }).produces(SyntheticBeanBuildItem.class).build();
            }
        };
    }

    @Test
    public void testBeanTypes() {
        ArcContainer container = Arc.container();
        Assertions.assertFalse(container.select(Foo.class).isResolvable());
        Assertions.assertTrue(container.select(FooInterface.class).isResolvable());
    }

    @Vetoed
    public static class Foo implements FooInterface {
    }

    interface FooInterface {
    }

    public static class FooCreator implements BeanCreator<Foo> {

        @Override
        public Foo create(SyntheticCreationalContext<Foo> context) {
            return new Foo();
        }

    }
}
