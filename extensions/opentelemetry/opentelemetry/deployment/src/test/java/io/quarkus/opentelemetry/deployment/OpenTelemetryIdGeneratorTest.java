package io.quarkus.opentelemetry.deployment;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;

import java.lang.reflect.InvocationTargetException;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.extension.aws.trace.AwsXrayIdGenerator;
import io.opentelemetry.sdk.trace.IdGenerator;
import io.quarkus.test.QuarkusUnitTest;

public class OpenTelemetryIdGeneratorTest {
    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClass(TestUtil.class));

    @Inject
    OpenTelemetry openTelemetry;

    @Test
    void test() throws NoSuchFieldException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        IdGenerator idGenerator = TestUtil.getIdGenerator(openTelemetry);

        assertThat(idGenerator, instanceOf(AwsXrayIdGenerator.class));
    }

    @ApplicationScoped
    public static class OtelConfiguration {

        @Produces
        public IdGenerator idGenerator() {
            return AwsXrayIdGenerator.getInstance();
        }
    }
}
