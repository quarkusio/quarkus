package org.jboss.shamrock.example.test;

import static org.junit.Assert.assertEquals;

import org.jboss.shamrock.test.ShamrockTest;
import org.jboss.shamrock.test.URLTester;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test various Bean Validation operations running in Shamrock
 */
@RunWith(ShamrockTest.class)
public class BeanValidationFunctionalityTest {

    @Test
    public void testBeanValidationFunctionalityFromServlet() throws Exception {
        StringBuilder expected = new StringBuilder();
        expected.append("failed: email (must be a well-formed email address)").append("\n");
        expected.append("passed");

        assertEquals(expected.toString(), URLTester.relative("bean-validation/testfunctionality").invokeURL().asString());
    }
}
