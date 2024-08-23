package io.quarkus.it.picocli;

import static io.quarkus.it.picocli.SystemPropertyCommand.PROPERTY_NAME;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import io.quarkus.test.junit.main.QuarkusMainTest;

@QuarkusMainTest
public class BeforeEachLaunchTest {

    private String previousValue;

    @BeforeEach
    void setUp() {
        previousValue = System.setProperty(PROPERTY_NAME, "beforeEach");
    }

    @AfterEach
    void tearDown() {
        if (previousValue == null) {
            System.clearProperty(PROPERTY_NAME);
        } else {
            System.setProperty(PROPERTY_NAME, previousValue);
        }
    }

    @Test
    @Launch({ "system-property-command" })
    public void testLaunchResult(LaunchResult result) {
        assertThat(result.getOutput()).isEqualTo("beforeEach");
    }

    @Test
    @Launch({ "system-property-command" })
    public void testLaunchAnnotationOnly() {

    }
}
