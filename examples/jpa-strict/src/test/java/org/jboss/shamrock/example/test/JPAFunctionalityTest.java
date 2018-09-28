package org.jboss.shamrock.example.test;

import static org.junit.Assert.assertEquals;

import org.jboss.shamrock.example.testutils.URLTester;
import org.jboss.shamrock.junit.GraalTest;
import org.jboss.shamrock.junit.ShamrockTest;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test various JPA operations running in Shamrock
 */
@RunWith(ShamrockTest.class)
public class JPAFunctionalityTest {

    @Test
    public void testJPAFunctionalityFromServlet() throws Exception {
        assertEquals("OK", URLTester.relative("jpa/testfunctionality").invokeURL().asString());
    }

}
