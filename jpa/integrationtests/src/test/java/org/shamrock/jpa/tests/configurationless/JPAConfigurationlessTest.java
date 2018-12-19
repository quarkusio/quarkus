package org.shamrock.jpa.tests.configurationless;

import org.jboss.shamrock.test.ShamrockTest;
import org.jboss.shamrock.test.URLTester;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.core.Is.is;

/**
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
@RunWith(ShamrockTest.class)
public class JPAConfigurationlessTest {

    @Test
    public void testInjection() {
        String result = URLTester.relative("jpa-test").invokeURL().asString();
        Assert.assertThat(result.contains("jpa=OK"), is(true));
    }
}
