package io.quarkus.qute.deployment.engineconfigurations.rawcontent;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Template;
import io.quarkus.test.QuarkusExtensionTest;

public class RawContentSectionTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root.addClasses(RawContentSectionFactory.class)
                    .addAsResource(new StringAsset("{#raw}Hello {name}! {illegal {/raw}"),
                            "templates/foo.html"));

    @Inject
    Template foo;

    @Test
    public void testRawContent() {
        assertEquals("HELLO {NAME}! {ILLEGAL ", foo.render());
    }

}
