package io.quarkus.arc.test.config.staticinit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class StaticInitSafeConfigInjectionTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().withApplicationRoot(
            root -> root.addClasses(StaticInitSafeBean.class, StaticInitLazyBean.class, UnsafeConfigSource.class)
                    .addAsServiceProvider(ConfigSource.class, UnsafeConfigSource.class)
                    // the value from application.properties should be injected during STATIC_INIT
                    .addAsResource(new StringAsset("apfelstrudel=jandex"), "application.properties"));

    @Inject
    StaticInitSafeBean bean;

    @Test
    public void test() {
        assertEquals("jandex", bean.value);
    }

}
