package org.jboss.shamrock.undertow.test;

import org.jboss.shamrock.test.Deployment;
import org.jboss.shamrock.test.ShamrockUnitTest;
import org.jboss.shamrock.test.URLResponse;
import org.jboss.shamrock.test.URLTester;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(ShamrockUnitTest.class)
public class ServletTestCase {

    @Deployment
    public static JavaArchive deploy() {
        return ShrinkWrap.create(JavaArchive.class)
                .addClasses(TestServlet.class);
    }

    @Test
    public void testServlet() {
        URLResponse resp = URLTester.relative("/test").invokeURL();
        Assert.assertEquals(200, resp.statusCode());
        Assert.assertEquals("test servlet", resp.asString());
    }

}
