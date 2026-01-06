package io.quarkus.vertx.mdc;

import static java.util.Collections.emptySet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.VertxContextSupport;
import io.quarkus.vertx.core.runtime.VertxMDC;
import io.vertx.core.Vertx;

public class AnotherVertxMdcTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(
                    () -> ShrinkWrap.create(JavaArchive.class)
                            .addClass(HelloBean.class))
            .overrideConfigKey("quarkus.log.console.format", "%d{HH:mm:ss} %-5p requestId=%X{requestId} [%c{2.}] (%t) %s%e%n");

    static final String VALUE_TO_PRESERVE = "value to preserve";
    static final String PROBE = "probe";

    @Inject
    HelloBean bean;

    @BeforeEach
    void setUp() {
        bean.reset();
    }

    @Test
    void reinitializationTest_nullKeysDiscarded() throws ExecutionException, InterruptedException, TimeoutException {
        bean.addEntriesMDC(PROBE, VALUE_TO_PRESERVE);
        Map<String, Object> report = bean.hello(null);

        assertNotNull(report.get("MDC with probe"));
        assertEquals(VALUE_TO_PRESERVE, unwrapMdcValue("MDC with probe", PROBE, report));
        assertEquals(VALUE_TO_PRESERVE, unwrapMdcValue("MDC after reinit", PROBE, report));

        assertNotEquals(report.get("MDC before object"), report.get("MDC after reinit object"));
    }

    @Test
    void reinitializationTest_noKeysToStore() throws ExecutionException, InterruptedException, TimeoutException {
        Map<String, Object> report = bean.hello(null);
        // nothing will be done.
        assertNull(report.get("MDC with probe"));
        assertNull(report.get("MDC after reinit"));
    }

    @Test
    void reinitializationTest_emptyKeysDiscarded() throws ExecutionException, InterruptedException, TimeoutException {
        bean.addEntriesMDC(PROBE, VALUE_TO_PRESERVE);
        Map<String, Object> report = bean.hello(emptySet());

        assertNotNull(report.get("MDC with probe"));
        assertEquals(VALUE_TO_PRESERVE, unwrapMdcValue("MDC with probe", PROBE, report));
        assertEquals(VALUE_TO_PRESERVE, unwrapMdcValue("MDC after reinit", PROBE, report));

        assertNotEquals(report.get("MDC before object"), report.get("MDC after reinit object"));
    }

    @Test
    void reinitializationTest_keysDiscarded() throws ExecutionException, InterruptedException, TimeoutException {
        bean.addEntriesMDC(PROBE, VALUE_TO_PRESERVE);
        bean.addEntriesMDC("keyToDiscard", "valueToDiscard");
        Map<String, Object> report = bean.hello(Set.of("keyToDiscard"));

        assertNotNull(report.get("MDC with probe"));
        assertEquals(VALUE_TO_PRESERVE, unwrapMdcValue("MDC with probe", PROBE, report));
        assertEquals("valueToDiscard", unwrapMdcValue("MDC with probe", "keyToDiscard", report));

        assertEquals(VALUE_TO_PRESERVE, unwrapMdcValue("MDC after reinit", PROBE, report));
        assertNull(unwrapMdcValue("MDC after reinit", "keyToDiscard", report));

        assertNotEquals(report.get("MDC before object"), report.get("MDC after reinit object"));
    }

    private String unwrapMdcValue(String reportEntry, String key, Object map) {
        Map<String, Object> mdc = (Map<String, Object>) ((Map<String, Object>) map).get(reportEntry);
        return (String) mdc.get(key);
    }

    @ApplicationScoped
    public static class HelloBean {
        private Map<String, String> map = new HashMap<>();

        public void addEntriesMDC(String key, String value) {
            map.put(key, value);
        }

        public void reset() {
            map.clear();
        }

        public Map<String, Object> hello(Set<String> keysToDiscard)
                throws ExecutionException, InterruptedException, TimeoutException {
            return VertxContextSupport.executeBlocking(() -> {
                Map<String, Object> report = new HashMap<>();

                for (Map.Entry entry : map.entrySet()) {
                    VertxMDC.INSTANCE.put((String) entry.getKey(), (String) entry.getValue());
                }
                report.put("MDC with probe", Vertx.currentContext().getLocal(VertxMDC.class.getName()));
                report.put("MDC before object",
                        "" + System.identityHashCode(Vertx.currentContext().getLocal(VertxMDC.class.getName())));

                VertxMDC.INSTANCE.reinitializeVertxMdc(Vertx.currentContext(), keysToDiscard);
                report.put("MDC after reinit", Vertx.currentContext().getLocal(VertxMDC.class.getName()));
                report.put("MDC after reinit object",
                        "" + System.identityHashCode(Vertx.currentContext().getLocal(VertxMDC.class.getName())));
                return report;
            }).subscribe().asCompletionStage().toCompletableFuture().get(1, TimeUnit.SECONDS);
        }
    }
}
