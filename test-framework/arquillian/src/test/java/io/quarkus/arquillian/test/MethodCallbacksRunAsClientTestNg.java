package io.quarkus.arquillian.test;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * The TestNG counterpart of {@link RunAsClientCallbacksTest}, run by {@link TestNgCallbacksTest} rather than by the build
 * itself, because the build runs the JUnit provider only. The counter is kept in a system property rather than in a
 * static field, because the two test instances are loaded by different class loaders and would therefore each have their
 * own copy of a static field, which would hide the second invocation.
 */
public class MethodCallbacksRunAsClientTestNg extends Arquillian {

    static final String BEFORE_METHOD_COUNT = MethodCallbacksRunAsClientTestNg.class.getName() + ".beforeMethodCount";

    @Deployment(testable = false)
    public static JavaArchive createTestArchive() {
        return ShrinkWrap.create(JavaArchive.class)
                .addClass(Foo.class);
    }

    @BeforeMethod
    public void beforeMethod() {
        System.setProperty(BEFORE_METHOD_COUNT, String.valueOf(count() + 1));
    }

    static int count() {
        return Integer.parseInt(System.getProperty(BEFORE_METHOD_COUNT, "0"));
    }

    @Test
    public void testRunsAsClient() {
    }
}
