package org.jboss.shamrock.example.test;

import static org.junit.Assert.assertEquals;

import org.jboss.shamrock.example.testutils.URLTester;
import org.jboss.shamrock.junit.GraalTest;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(GraalTest.class)
public class CoreReflectionInGraalITCase {

    @Test
    public void testFieldAndGetterReflectionOnEntityFromServlet() throws Exception {
        assertEquals("OK", URLTester.relative("core/reflection").invokeURL().asString());
    }

}
