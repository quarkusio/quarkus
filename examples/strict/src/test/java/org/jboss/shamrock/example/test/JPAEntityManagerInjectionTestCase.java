package org.jboss.shamrock.example.test;

import static org.junit.Assert.assertEquals;

import org.jboss.shamrock.example.testutils.URLTester;
import org.jboss.shamrock.junit.ShamrockTest;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test reflection around JPA entities
 *
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
@RunWith(ShamrockTest.class)
public class JPAEntityManagerInjectionTestCase {

    @Test
    public void testJpaEntityManagerInjection() throws Exception {
        assertEquals("OK", URLTester.relative("jpa/testjpaeminjection").invokeURL().asString());
    }

}
