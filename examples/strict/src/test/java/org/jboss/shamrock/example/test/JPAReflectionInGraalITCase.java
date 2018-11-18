package org.jboss.shamrock.example.test;

import static org.junit.Assert.assertEquals;

import org.jboss.shamrock.example.testutils.URLTester;
import org.jboss.shamrock.test.SubstrateTest;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test reflection around JPA entities
 *
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
@RunWith(SubstrateTest.class)
public class JPAReflectionInGraalITCase {

    @Test
    public void testFieldAndGetterReflectionOnEntityFromServlet() throws Exception {
        assertEquals("OK", URLTester.relative("jpa/testreflection").invokeURL().asString());
    }

}
