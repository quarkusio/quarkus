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
        expected.append("failed: additionalEmails[0].<list element> (must be a well-formed email address)").append(", ")
                .append("categorizedEmails<K>[a].<map key> (length must be between 3 and 2147483647)").append(", ")
                .append("categorizedEmails[a].<map value>[0].<list element> (must be a well-formed email address)").append(", ")
                .append("email (must be a well-formed email address)").append(", ")
                .append("score (must be greater than or equal to 0)").append("\n");
        expected.append("passed");

        assertEquals(expected.toString(), URLTester.relative("bean-validation/testfunctionality").invokeURL().asString());
    }
}
