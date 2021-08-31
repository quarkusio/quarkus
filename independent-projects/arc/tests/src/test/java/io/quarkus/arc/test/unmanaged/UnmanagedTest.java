package io.quarkus.arc.test.unmanaged;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.arc.test.ArcTestContainer;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.Unmanaged;
import javax.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class UnmanagedTest {
    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(UnmanagedTest.Foo.class, UnmanagedTest.Bar.class);

    @Test
    public void testUnmanaged() {
        Unmanaged.UnmanagedInstance<UnmanagedTest.Foo> unmanaged = new Unmanaged<>(UnmanagedTest.Foo.class).newInstance();
        UnmanagedTest.Foo foo = unmanaged.produce()
                .inject()
                .postConstruct()
                .get();

        assertNotNull(foo);
        assertNotNull(foo.getBar());

        assertTrue(foo.postConstructCalled);
        assertFalse(foo.preDestroyCalled);

        unmanaged.preDestroy().dispose();

        assertTrue(foo.preDestroyCalled);
    }

    @ApplicationScoped
    static class Bar {

    }

    @ApplicationScoped
    static class Foo {

        public static boolean postConstructCalled = false;
        public static boolean preDestroyCalled = false;

        @Inject
        Bar bar;

        public Bar getBar() {
            return bar;
        }

        @PostConstruct
        public void init() {
            postConstructCalled = true;
        }

        @PreDestroy
        public void destroy() {
            preDestroyCalled = true;
        }

        public static void reset() {
            postConstructCalled = false;
            preDestroyCalled = false;
        }
    }
}
