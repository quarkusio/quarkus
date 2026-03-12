package io.quarkus.qute.deployment.inject;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Template;
import io.quarkus.test.QuarkusExtensionTest;

public class NamedBeanNotFoundSafeTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root
                    .addAsResource(new StringAsset("{cdi:ping.val??}"), "templates/ping.html"));

    @Inject
    Template ping;

    @Test
    public void testTemplate() {
        assertEquals("", ping.render());
    }

}
