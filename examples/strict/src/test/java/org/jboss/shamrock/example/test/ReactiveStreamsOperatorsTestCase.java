package org.jboss.shamrock.example.test;

import org.jboss.shamrock.example.testutils.URLTester;
import org.jboss.shamrock.test.ShamrockTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(ShamrockTest.class)
public class ReactiveStreamsOperatorsTestCase {

    @Test
    public void testReactiveStreams() {
        String result = URLTester.relative("rest/reactive/stream-regular").invokeURL().asString();
        Assert.assertEquals("ABC", result);
    }

    @Test
    public void testRxJava2() {
        String result = URLTester.relative("rest/reactive/stream-rx").invokeURL().asString();
        Assert.assertEquals("DEF", result);
    }

}
