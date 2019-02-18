package org.jboss.shamrock.extest;

import javax.inject.Inject;

import org.jboss.shamrock.test.ShamrockUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Test driver for the test-extension
 */
public class ConfiguredBeanTest {
    @RegisterExtension
    static final ShamrockUnitTest config = new ShamrockUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(ConfiguredBean.class)
                    .addAsManifestResource("microprofile-config.properties")
            );

    @Inject
    ConfiguredBean configuredBean;

    /**
     * All of the testing is currently done in the
     */
    @Test
    public void validateConfiguredBean() {
        System.out.printf("validateConfiguredBean, %s\n", configuredBean);
    }
}
