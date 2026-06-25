package io.quarkus.http3.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;
import io.vertx.ext.web.Router;

class Http3ProdModeTlsWarningTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot(jar -> jar.addClasses(MyBean.class))
            .setApplicationName("http3-prod-tls-warning")
            .setApplicationVersion("1.0.0")
            .setRun(false)
            .setLogRecordPredicate(r -> r.getLoggerName().contains("Http3Processor")
                    && r.getLevel().intValue() >= Level.WARNING.intValue());

    @ProdBuildResults
    private ProdModeTestResults prodModeTestResults;

    @Test
    void testWarningLoggedWhenNoTlsInProdMode() {
        List<LogRecord> records = prodModeTestResults.getRetainedBuildLogRecords();
        assertThat(records).isNotEmpty();
        assertThat(records).anyMatch(r -> r.getMessage().contains("HTTP/3 is enabled but no TLS configuration was detected")
                && r.getMessage().contains("HTTP/3 requires TLS"));
    }

    @ApplicationScoped
    static class MyBean {
        public void register(@Observes Router router) {
            router.get("/hello").handler(rc -> rc.response().end("hello"));
        }
    }
}
