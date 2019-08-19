package io.quarkus.arc.test.injection.erroneous;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import io.quarkus.arc.test.ArcTestContainer;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class CircularInjectionNotSupportedTest {

    @Rule
    public RuleChain chain = RuleChain.outerRule(new TestRule() {
        @Override
        public Statement apply(Statement base, Description description) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    try {
                        base.evaluate();
                        fail("Expected an IllegalStateException to be thrown, but it wasn't");
                    } catch (IllegalStateException e) {
                        // expected failure on ISE due to circular dependency
                        assertThat(e.getMessage(), containsString("Circular dependencies not supported"));
                    }
                }
            };
        }
    }).around(new ArcTestContainer(Foo.class, AbstractServiceImpl.class, ActualServiceImpl.class));

    @Test
    public void testExceptionThrown() {
        // throws exception during deployment
    }

    static abstract class AbstractServiceImpl {
        @Inject
        protected Foo foo;
    }

    @Singleton
    static class ActualServiceImpl extends AbstractServiceImpl implements Foo {

        @Override
        public void ping() {
        }
    }

    interface Foo {
        void ping();
    }

}
