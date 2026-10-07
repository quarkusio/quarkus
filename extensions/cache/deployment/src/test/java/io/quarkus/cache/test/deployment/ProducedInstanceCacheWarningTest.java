package io.quarkus.cache.test.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.logging.LogRecord;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.cache.CacheResult;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * CDI does not apply interceptors to the values of producer methods and fields, so cache annotations on such an
 * instance are silently ignored; the build must say so.
 */
public class ProducedInstanceCacheWarningTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar.addClasses(Service.class, ServiceImpl.class, Producers.class,
                    ManagedService.class))
            .setLogRecordPredicate(r -> r.getMessage() != null && r.getMessage().contains("not intercepted"))
            .assertLogRecords(records -> {
                assertEquals(1, records.size(), records.toString());
                String message = format(records.get(0));
                assertTrue(message.contains(ServiceImpl.class.getName() + "#get"), message);
                assertTrue(message.contains(Producers.class.getName() + "#service"), message);
            });

    private static String format(LogRecord record) {
        return record.getParameters() == null ? record.getMessage()
                : String.format(record.getMessage(), record.getParameters());
    }

    @Inject
    Service service;

    @Inject
    ManagedService managedService;

    @Test
    public void producedInstanceIsNotCached() {
        assertEquals(1, service.get());
        assertEquals(2, service.get());
    }

    @Test
    public void managedBeanIsCached() {
        assertEquals(1, managedService.get());
        assertEquals(1, managedService.get());
    }

    interface Service {
        int get();
    }

    static class ServiceImpl implements Service {

        private int calls;

        @CacheResult(cacheName = "produced-instance")
        public int get() {
            return ++calls;
        }
    }

    @ApplicationScoped
    static class Producers {

        @Produces
        @ApplicationScoped
        Service service() {
            return new ServiceImpl();
        }
    }

    @ApplicationScoped
    static class ManagedService {

        private int calls;

        @CacheResult(cacheName = "managed-bean")
        public int get() {
            return ++calls;
        }
    }
}
