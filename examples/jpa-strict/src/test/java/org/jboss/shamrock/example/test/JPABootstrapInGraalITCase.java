package org.jboss.shamrock.example.test;

import org.jboss.shamrock.example.testutils.URLTester;
import org.jboss.shamrock.junit.GraalTest;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

/**
 * Test reflection around JPA entities
 *
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
@RunWith(GraalTest.class)
public class JPABootstrapInGraalITCase {

    @Test
    public void testFieldAndGetterReflectionOnEntityFromServlet() throws Exception {
        assertEquals("OK", URLTester.relative("jpa/testbootstrap").invokeURL().asString());
    }

}
