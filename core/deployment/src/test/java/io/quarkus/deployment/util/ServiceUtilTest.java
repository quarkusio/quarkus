
package io.quarkus.deployment.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.Set;

import org.junit.jupiter.api.Test;

public class ServiceUtilTest {

    /**
     * Test that shows how comments (#) are ignored by {@link ServiceUtil}. See
     * Issue #2892.
     */
    @Test
    public void testNamesNamedIn() throws IOException {
        String fileName = "META-INF/services/test.javax.servlet.ServletContainerInitializer";
        String[] classNames = new String[] {
                "org.apache.logging.log4j.web.Log4jServletContainerInitializer",
                "org.apache.logging.log4j.core.Appender",
                "org.apache.logging.log4j.core.Core",
                "org.apache.logging.log4j.core.Core2",
                "org.apache.logging.log4j.core.Core3",
                "org.apache.logging.log4j.core.Layout",
                "org.apache.logging.log4j.core.Layout2"
        };

        Set<String> classes = ServiceUtil.classNamesNamedIn(Thread.currentThread().getContextClassLoader(), fileName);

        assertEquals(7, classes.size());

        assertThat(classes).contains(classNames);

    }
}
