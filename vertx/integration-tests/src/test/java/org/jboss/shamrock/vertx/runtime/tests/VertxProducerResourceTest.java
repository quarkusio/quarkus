package org.jboss.shamrock.vertx.runtime.tests;

import org.jboss.shamrock.test.ShamrockTest;
import org.jboss.shamrock.test.URLTester;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.core.Is.is;

@RunWith(ShamrockTest.class)
public class VertxProducerResourceTest {


    @Test
    public void testInjection() {
        String result = URLTester.relative("vertx-test").invokeURL().asString();
        Assert.assertThat(result.contains("vertx=true"), is(true));
        Assert.assertThat(result.contains("eventbus=true"), is(true));
    }

    @Test
    public void testEventBus() {
        String result = URLTester.relative("vertx-test/eventBus").invokeURL().asString();
        Assert.assertThat(result.contains("hello shamrock"), is(true));
    }

}