package io.quarkus.qute.deployment.extensions;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateExtension;
import io.quarkus.test.QuarkusUnitTest;

/**
 * https://github.com/quarkusio/quarkus/issues/26306
 */
public class AssignabilityTemplateExtensionTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClass(Extensions.class)
                    .addAsResource(new StringAsset(
                            "{@java.time.LocalDateTime foo} {datetime:formatDate(foo)}"),
                            "templates/foo.html"));

    @Inject
    Template foo;

    @Test
    public void testFormat() {
        assertTrue(!foo.data("foo", LocalDateTime.now()).render().isBlank());
    }

    @TemplateExtension(namespace = "datetime")
    public static class Extensions {

        public static String formatDate(TemporalAccessor date) {
            return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(date);
        }

    }

}
