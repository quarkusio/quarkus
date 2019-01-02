package org.shamrock.jpa.tests.configurationless;

import org.jboss.shamrock.test.ShamrockTest;
import org.jboss.shamrock.test.URLTester;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.core.StringContains.containsString;

/**
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
@RunWith(ShamrockTest.class)
public class JPAConfigurationlessTest {

    @Test
    public void testInjection() {
        String result = URLTester.relative("jpa-test").invokeURL().asString();
        Assert.assertThat(result, containsString("jpa=OK"));
        result = URLTester.relative("jpa-test/user-tx").invokeURL().asString();
        Assert.assertThat(result, containsString("jpa=OK"));
    }
}
