package io.quarkus.resteasy.reactive.server.test.resource.basic;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.function.Supplier;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Unremovable;
import io.quarkus.test.QuarkusUnitTest;

public class ContextAndInstanceTest {

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    JavaArchive jar = ShrinkWrap.create(JavaArchive.class);
                    jar.addClasses(SummaryGeneratorInterface.class);
                    jar.addClasses(SummaryGeneratorSubInterface.class);
                    jar.addClasses(SummaryGenerator.class);
                    jar.addClasses(GermanSummaryGenerator.class);
                    jar.addClasses(GreetingGeneratorInterface.class);
                    jar.addClasses(GreetingGeneratorSubInterface.class);
                    jar.addClasses(GermanGreetingGenerator.class);
                    return jar;
                }
            });

    @Test
    void testContextWithAbstractClass() {
        {
            Instance<GermanSummaryGenerator> greetingGenerators = CDI.current()
                    .select(ContextAndInstanceTest.GermanSummaryGenerator.class);
            assertThat(greetingGenerators.isResolvable(), equalTo(true));
        }

        {
            Instance<SummaryGenerator> greetingGenerators = CDI.current()
                    .select(ContextAndInstanceTest.SummaryGenerator.class);
            assertThat(greetingGenerators.isResolvable(), equalTo(true));
        }

        {
            Instance<SummaryGeneratorInterface> greetingGenerators = CDI.current()
                    .select(ContextAndInstanceTest.SummaryGeneratorInterface.class);
            assertThat(greetingGenerators.isResolvable(), equalTo(true));
        }

        {
            Instance<SummaryGeneratorSubInterface> greetingGenerators = CDI.current()
                    .select(ContextAndInstanceTest.SummaryGeneratorSubInterface.class);
            assertThat(greetingGenerators.isResolvable(), equalTo(true));
        }
    }

    @Test
    void testContextWithOnlyInterfaces() {
        {
            Instance<GreetingGeneratorInterface> greetingGenerators = CDI.current()
                    .select(ContextAndInstanceTest.GreetingGeneratorInterface.class);
            assertThat(greetingGenerators.isResolvable(), equalTo(true));
        }

        {
            Instance<GreetingGeneratorSubInterface> greetingGenerators = CDI.current()
                    .select(ContextAndInstanceTest.GreetingGeneratorSubInterface.class);
            assertThat(greetingGenerators.isResolvable(), equalTo(true));
        }

        {
            Instance<AnotherSubInterface> greetingGenerators = CDI.current()
                    .select(ContextAndInstanceTest.AnotherSubInterface.class);
            assertThat(greetingGenerators.isResolvable(), equalTo(true));
        }

        {
            Instance<AnotherInterface> greetingGenerators = CDI.current()
                    .select(ContextAndInstanceTest.AnotherInterface.class);
            assertThat(greetingGenerators.isResolvable(), equalTo(true));
        }
    }

    public interface SummaryGeneratorInterface {
    }

    public interface SummaryGeneratorSubInterface extends SummaryGeneratorInterface {
    }

    public abstract static class SummaryGenerator implements SummaryGeneratorSubInterface {
    }

    @Unremovable
    @ApplicationScoped
    public static class GermanSummaryGenerator extends SummaryGenerator {

        @Context
        HttpHeaders headers;
    }

    public interface GreetingGeneratorInterface {
    }

    public interface GreetingGeneratorSubInterface extends GreetingGeneratorInterface {
    }

    public interface AnotherInterface {
    }

    public interface AnotherSubInterface extends AnotherInterface {
    }

    @Unremovable
    @ApplicationScoped
    public static class GermanGreetingGenerator implements GreetingGeneratorSubInterface, AnotherSubInterface {

        @Context
        HttpHeaders headers;
    }
}
