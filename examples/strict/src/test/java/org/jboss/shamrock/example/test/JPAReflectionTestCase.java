package org.jboss.shamrock.example.test;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.jboss.shamrock.junit.ShamrockTest;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test reflection around JPA entities
 *
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
@RunWith(ShamrockTest.class)
public class JPAReflectionTestCase {

    @Test
    public void testFieldAndGetterReflectionOnEntity() throws Exception {
        Class<?> custClass = Class.forName("org.jboss.shamrock.example.jpa.Customer");
        Object instance = custClass.newInstance();
        Field id = custClass.getDeclaredField("id");
        id.setAccessible(true);
        assertEquals("id should be reachable and null", null, id.get(instance));
        Method setter = custClass.getDeclaredMethod("setName", String.class);
        Method getter = custClass.getDeclaredMethod("getName");
        setter.invoke(instance, "Emmanuel");
        assertEquals("getter / setter should be reachable and usable", "Emmanuel", getter.invoke(instance));
    }

}
