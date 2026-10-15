package io.quarkus.observation.opentelemetry.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.enterprise.context.ApplicationScoped;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.micrometer.observation.annotation.Observed;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * An odd number of {@code @Observed.lowCardinalityKeyValues} means a key without a value. The build
 * must fail fast rather than silently dropping the trailing key at runtime.
 */
public class ObservedOddKeyValuesTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(OddKeyValuesBean.class))
            .assertException(t -> assertThat(t)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("lowCardinalityKeyValues")
                    .hasMessageContaining("even number")
                    .hasMessageContaining("oddKeyValues"));

    @Test
    void buildShouldHaveFailed() {
        Assertions.fail("Build should have failed because of the odd number of lowCardinalityKeyValues");
    }

    @ApplicationScoped
    public static class OddKeyValuesBean {

        @Observed(lowCardinalityKeyValues = { "env" })
        public String oddKeyValues() {
            return "kv";
        }
    }
}
