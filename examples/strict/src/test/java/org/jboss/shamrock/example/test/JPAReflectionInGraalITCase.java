package org.jboss.shamrock.example.test;

import static org.junit.Assert.assertEquals;

import org.jboss.shamrock.example.testutils.URLTester;
import org.jboss.shamrock.junit.GraalTest;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test reflection around JPA entities
 *
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
@RunWith(GraalTest.class)
public class JPAReflectionInGraalITCase {

    @Test
    public void testFieldAndGetterReflectionOnEntityFromServlet() throws Exception {
        assertEquals("OK", URLTester.relative("jpa/test").invokeURL().asString());
    }

}
