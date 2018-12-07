package org.jboss.shamrock.example.test;

import static org.junit.Assert.assertEquals;

import org.jboss.shamrock.example.testutils.H2DatabaseLifecycleManager;
import org.jboss.shamrock.test.ShamrockTest;
import org.jboss.shamrock.test.URLTester;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test connecting Hibernate ORM to H2.
 * The H2 database server is run in JVM mode, the Hibernate based application
 * is run in both JVM mode and native mode (see also test in subclass).
 */
@RunWith(ShamrockTest.class)
public class JPAFunctionalityTest {

    @ClassRule
    public static final H2DatabaseLifecycleManager h2 = new H2DatabaseLifecycleManager();

    @Test
    public void testJPAFunctionalityFromServlet() throws Exception {
        assertEquals("OK", URLTester.relative("jpa-h2/testfunctionality").invokeURL().asString());
    }

}
