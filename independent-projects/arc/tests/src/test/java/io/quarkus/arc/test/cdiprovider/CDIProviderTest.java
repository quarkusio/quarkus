package io.quarkus.arc.test.cdiprovider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.arc.test.ArcTestContainer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.spi.CDI;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class CDIProviderTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Moo.class);

    @Test
    public void testProducer() throws IOException {
        Moo moo = CDI.current()
                .select(Moo.class)
                .get();
        assertEquals(10, moo.getVal());
    }

    @AfterAll
    public static void unset() {
        assertTrue(Moo.DESTROYED.get());
        try {
            Field providerField = CDI.class.getDeclaredField("configuredProvider");
            providerField.setAccessible(true);
            providerField.set(null, null);
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    @Dependent
    static class Moo {

        private int val;

        static final AtomicBoolean DESTROYED = new AtomicBoolean();

        @PostConstruct
        void init() {
            val = 10;
        }

        @PreDestroy
        void destroy() {
            DESTROYED.set(true);
        }

        int getVal() {
            return val;
        }

    }

}
