package io.quarkus.qute.deployment.encoding;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Template;
import io.quarkus.test.QuarkusExtensionTest;

public class CustomEncodingTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root
                    .addAsResource("io/quarkus/qute/deployment/encoding/foo.txt", "templates/foo.txt"))
            .overrideConfigKey("quarkus.qute.default-charset", "windows-1250");

    @Inject
    Template foo;

    @Test
    public void testEncoding() {
        assertEquals("kočka", foo.render().strip());
    }

}
