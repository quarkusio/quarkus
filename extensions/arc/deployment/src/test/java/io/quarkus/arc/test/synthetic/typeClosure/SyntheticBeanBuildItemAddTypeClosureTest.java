package io.quarkus.arc.test.synthetic.typeClosure;

import java.util.function.Consumer;

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

public class SyntheticBeanBuildItemAddTypeClosureTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(SyntheticBeanBuildItemAddTypeClosureTest.class, FooCreator.class, FooInterface.class, Foo.class,
                            FooSubclass.class, Charlie.class, CharlieSubclass.class, CharlieInterface.class, BarInterface.class,
                            BazInterface.class))
            .addBuildChainCustomizer(buildCustomizer());

    static Consumer<BuildChainBuilder> buildCustomizer() {
        return new Consumer<BuildChainBuilder>() {

            @Override
            public void accept(BuildChainBuilder builder) {
                builder.addBuildStep(new BuildStep() {

                    @Override
                    public void execute(BuildContext context) {
                        context.produce(SyntheticBeanBuildItem.create(FooSubclass.class)
                                .addTypeClosure(FooInterface.class)
                                .addTypeClosure(DotName.createSimple(Foo.class))
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
    public void testBeanTypesDiscovered() {
        ArcContainer container = Arc.container();
        Assertions.assertTrue(container.select(Foo.class).isResolvable());
        Assertions.assertTrue(container.select(FooInterface.class).isResolvable());
        Assertions.assertTrue(container.select(BarInterface.class).isResolvable());
        Assertions.assertTrue(container.select(Charlie.class).isResolvable());
        Assertions.assertTrue(container.select(CharlieInterface.class).isResolvable());
        Assertions.assertTrue(container.select(BazInterface.class).isResolvable());

        // Charlie and Foo subclasses should not be registered as bean types
        Assertions.assertFalse(container.select(CharlieSubclass.class).isResolvable());
        Assertions.assertFalse(container.select(FooSubclass.class).isResolvable());
    }

    public static class FooSubclass extends Foo implements FooInterface {
    }

    public static class Foo extends Charlie implements BazInterface {
    }

    public static class CharlieSubclass extends Charlie {
    }

    public static class Charlie implements CharlieInterface {
    }

    interface CharlieInterface {
    }

    interface FooInterface extends BarInterface {
    }

    interface BarInterface {
    }

    interface BazInterface {
    }

    public static class FooCreator implements BeanCreator<FooSubclass> {

        @Override
        public FooSubclass create(SyntheticCreationalContext<FooSubclass> context) {
            return new FooSubclass();
        }

    }
}
