package io.quarkus.arc.test.config.staticinit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class StaticInitConfigInjectionFailureTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(root -> root
                    .addClasses(StaticInitBean.class, StaticInitEagerBean.class, UnsafeConfigSource.class)
                    .addAsServiceProvider(ConfigSource.class, UnsafeConfigSource.class)
                    // the value from application.properties should be injected during STATIC_INIT
                    .addAsResource(new StringAsset("apfelstrudel=jandex"), "application.properties"))
            .assertException(t -> {
                assertThat(t).isInstanceOf(IllegalStateException.class)
                        .hasMessageContainingAll(
                                "A runtime config property value differs from the value that was injected during the static intialization phase",
                                "the runtime value of 'apfelstrudel' is [gizmo] but the value [jandex] was injected into io.quarkus.arc.test.config.staticinit.StaticInitBean#value",
                                "the runtime value of 'apfelstrudel' is [gizmo] but the value [jandex] was injected into io.quarkus.arc.test.config.staticinit.StaticInitEagerBean#value");
            });

    @Test
    public void test() {
        fail();
    }

}
