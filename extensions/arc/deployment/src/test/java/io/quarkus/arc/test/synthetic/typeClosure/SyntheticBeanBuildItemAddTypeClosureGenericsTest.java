package io.quarkus.arc.test.synthetic.typeClosure;

import java.util.function.Consumer;

import jakarta.enterprise.util.TypeLiteral;

import org.jboss.jandex.ParameterizedType;
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

public class SyntheticBeanBuildItemAddTypeClosureGenericsTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(SyntheticBeanBuildItemAddTypeClosureGenericsTest.class, FooCreator.class, FooInterface.class,
                            Foo.class,
                            FooSubclass.class, Charlie.class, CharlieSubclass.class, CharlieInterface.class, BarInterface.class,
                            BazInterface.class, Alpha.class, Beta.class))
            .addBuildChainCustomizer(buildCustomizer());

    static Consumer<BuildChainBuilder> buildCustomizer() {
        return new Consumer<BuildChainBuilder>() {

            @Override
            public void accept(BuildChainBuilder builder) {
                builder.addBuildStep(new BuildStep() {

                    @Override
                    public void execute(BuildContext context) {
                        context.produce(SyntheticBeanBuildItem.create(FooSubclass.class)
                                .addTypeClosure(ParameterizedType.builder(FooSubclass.class).addArgument(Beta.class).build())
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

        // Foo/Bar/Baz interfaces should work normally, no generics there
        Assertions.assertTrue(container.select(FooInterface.class).isResolvable());
        Assertions.assertTrue(container.select(BarInterface.class).isResolvable());
        Assertions.assertTrue(container.select(BazInterface.class).isResolvable());

        // FooSubclass is resolvable only as correct parameterized type
        Assertions.assertTrue(container.select(new TypeLiteral<FooSubclass<Beta>>() {
        }).isResolvable());
        Assertions.assertFalse(container.select(new TypeLiteral<FooSubclass<Charlie>>() {
        }).isResolvable());
        Assertions.assertFalse(container.select(FooSubclass.class).isResolvable());

        // Foo type should work only parameterized
        Assertions.assertTrue(container.select(new TypeLiteral<Foo<Alpha, Beta>>() {
        }).isResolvable());
        Assertions.assertFalse(container.select(Foo.class).isResolvable());

        // Foo extends Charlie raw type
        // we should be able to perform resolution for raw type but not for a parameterized type
        Assertions.assertTrue(container.select(Charlie.class).isResolvable());
        Assertions.assertTrue(container.select(CharlieInterface.class).isResolvable());
        Assertions.assertFalse(container.select(new TypeLiteral<Charlie<String>>() {
        }).isResolvable());
        Assertions.assertFalse(container.select(new TypeLiteral<CharlieInterface<String>>() {
        }).isResolvable());

        // CharlieSubclass should not be discovered as bean type
        Assertions.assertFalse(container.select(CharlieSubclass.class).isResolvable());
    }

    public static class Alpha {

    }

    public static class Beta {

    }

    public static class FooSubclass<T> extends Foo<Alpha, Beta> implements FooInterface {
    }

    public static class Foo<A, B> extends Charlie implements BazInterface {
    }

    public static class CharlieSubclass<T> extends Charlie<String> {
    }

    public static class Charlie<T> implements CharlieInterface<T> {
    }

    interface CharlieInterface<T> {
    }

    interface FooInterface extends BarInterface {
    }

    interface BarInterface {
    }

    interface BazInterface {
    }

    public static class FooCreator implements BeanCreator<FooSubclass<Beta>> {

        @Override
        public FooSubclass<Beta> create(SyntheticCreationalContext<FooSubclass<Beta>> context) {
            return new FooSubclass<Beta>();
        }

    }
}
