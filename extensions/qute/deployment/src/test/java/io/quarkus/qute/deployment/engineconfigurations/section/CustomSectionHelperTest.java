package io.quarkus.qute.deployment.engineconfigurations.section;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Template;
import io.quarkus.test.QuarkusExtensionTest;

public class CustomSectionHelperTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root.addClasses(CustomSectionFactory.class, StringProducer.class)
                    .addAsResource(new StringAsset("{#custom foo=1 /}"), "templates/foo.html"));

    @Inject
    Template foo;

    @Test
    public void testSectionHelper() {
        assertEquals("1:BAR!", foo.render());
    }

}
