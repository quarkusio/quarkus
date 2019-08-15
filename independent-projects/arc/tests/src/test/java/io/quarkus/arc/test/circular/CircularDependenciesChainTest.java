package io.quarkus.arc.test.circular;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import io.quarkus.arc.test.ArcTestContainer;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import org.junit.Rule;
import org.junit.Test;

public class CircularDependenciesChainTest {
    @Rule
    public ArcTestContainer container = new ArcTestContainer(
            Foo.class,
            Bar.class,
            Baz.class);

    @Test
    public void testDependencies() {
        Foo foo = CDI.current().select(Foo.class).get();
        assertThat(foo, is(notNullValue(Foo.class)));
        assertThat(foo.ping(), is("foo is not null"));
    }

    @ApplicationScoped
    static class Foo {
        @Inject
        Bar bar;

        String ping() {
            return bar.ping();
        }
    }

    @ApplicationScoped
    static class Bar {
        @Inject
        Baz baz;

        String ping() {
            return baz.ping();
        }
    }

    @ApplicationScoped
    static class Baz {
        @Inject
        Foo foo;

        String ping() {
            return foo == null ? "foo is null" : "foo is not null";
        }
    }
}
