package io.quarkus.arquillian.test;

import static org.testng.Assert.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * The TestNG counterpart of {@link BeforeAllInContainerTest}, run by {@link TestNgCallbacksTest} rather than by the build
 * itself, because the build runs the JUnit provider only.
 * <p>
 * The {@code @BeforeClass} counter is static, so the value which the test method observes is the one of this class as
 * loaded by the application class loader, which is only incremented when the callback is invoked on the in container test
 * instance. The {@code @AfterClass} invocation can only be observed once the class has finished, by which time the
 * application class loader is gone, so it records the class loader which invoked it in a system property instead. TestNG
 * itself invokes the callback on the test instance which it created, so the mere fact that it was invoked proves nothing.
 */
public class ClassCallbacksTestNg extends Arquillian {

    static final String AFTER_CLASS_CLASS_LOADERS = ClassCallbacksTestNg.class.getName() + ".afterClassClassLoaders";

    static final AtomicInteger BEFORE_CLASS = new AtomicInteger();

    @Deployment
    public static JavaArchive createTestArchive() {
        return ShrinkWrap.create(JavaArchive.class)
                .addClass(Foo.class);
    }

    @Inject
    Foo foo;

    @BeforeClass
    public void beforeClass() {
        BEFORE_CLASS.incrementAndGet();
    }

    @AfterClass
    public void afterClass() {
        System.setProperty(AFTER_CLASS_CLASS_LOADERS,
                System.getProperty(AFTER_CLASS_CLASS_LOADERS, "") + getClass().getClassLoader().getClass().getName() + " ");
    }

    @Test
    public void testBeforeClassWasInvokedInContainer() {
        assertEquals(BEFORE_CLASS.get(), 1);
    }
}
