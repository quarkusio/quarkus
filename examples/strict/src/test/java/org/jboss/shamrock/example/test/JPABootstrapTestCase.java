package org.jboss.shamrock.example.test;

import static org.junit.Assert.assertEquals;

import org.jboss.shamrock.test.URLTester;
import org.jboss.shamrock.test.ShamrockTest;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test reflection around JPA entities
 *
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
@RunWith(ShamrockTest.class)
public class JPABootstrapTestCase {

    @Test
    public void testJpaBootstrap() throws Exception {
        assertEquals("OK", URLTester.relative("jpa/testbootstrap").invokeURL().asString());
    }

}
