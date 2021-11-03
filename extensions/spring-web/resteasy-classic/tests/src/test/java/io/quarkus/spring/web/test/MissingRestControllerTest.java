package io.quarkus.spring.web.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.logging.LogRecord;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class MissingRestControllerTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(NonAnnotatedController.class, ProperController.class))
            .setApplicationName("missing-rest-controller")
            .setApplicationVersion("0.1-SNAPSHOT")
            .setLogRecordPredicate(
                    r -> "io.quarkus.spring.web.deployment.SpringWebResteasyClassicProcessor".equals(r.getLoggerName()));

    @ProdBuildResults
    private ProdModeTestResults prodModeTestResults;

    @Test
    public void testBuildLogs() {
        List<LogRecord> buildLogRecords = prodModeTestResults.getRetainedBuildLogRecords();
        assertThat(buildLogRecords).isNotEmpty();
        assertThat(buildLogRecords).singleElement().satisfies(r -> {
            assertThat(r.getMessage())
                    .contains("a mapping annotation but the class itself")
                    .contains(NonAnnotatedController.class.getName())
                    .doesNotContain(ProperController.class.getName());
        });
    }

    @RequestMapping("/non")
    public static class NonAnnotatedController {

        @GetMapping("/hello")
        public String greet() {
            return "hello";
        }
    }

    @RestController
    @RequestMapping("/proper")
    public static class ProperController {

        @GetMapping("/hello")
        public String greet() {
            return "hello";
        }
    }
}
