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

public class CircularDependenciesTest {
    @Rule
    public ArcTestContainer container = new ArcTestContainer(
            AbstractServiceImpl.class,
            ActualServiceImpl.class,
            Foo.class);

    @Test
    public void testDependencies() {
        Foo foo = CDI.current().select(Foo.class).get();
        assertThat(foo, is(notNullValue(Foo.class)));
        assertThat(foo.ping(), is("pong"));
    }

    static abstract class AbstractServiceImpl {
        @Inject
        protected Foo foo;
    }

    @ApplicationScoped
    static class ActualServiceImpl extends AbstractServiceImpl implements Foo {

        @Override
        public String ping() {
            return "pong";
        }
    }

    interface Foo {
        String ping();
    }
}
