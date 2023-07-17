package io.quarkus.opentelemetry.deployment;

import java.lang.reflect.InvocationTargetException;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.OpenTelemetry;
// import io.opentelemetry.contrib.awsxray.AwsXrayIdGenerator;
import io.opentelemetry.sdk.trace.IdGenerator;
import io.quarkus.opentelemetry.deployment.common.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

@Disabled("We need to move the AWS dependency testing to an independent module")
public class OpenTelemetryIdGeneratorTest {
    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClass(TestUtil.class));

    @Inject
    OpenTelemetry openTelemetry;

    @Test
    void test() throws NoSuchFieldException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        IdGenerator idGenerator = TestUtil.getIdGenerator(openTelemetry);

        //        assertThat(idGenerator, instanceOf(AwsXrayIdGenerator.class));
    }

    //    @ApplicationScoped
    //    public static class OtelConfiguration {
    //
    //        @Produces
    //        public IdGenerator idGenerator() {
    //            return AwsXrayIdGenerator.getInstance();
    //        }
    //    }
}
