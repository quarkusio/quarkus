package io.quarkus.qute.deployment.i18n;

import static io.quarkus.qute.i18n.Message.UNDERSCORED_ELEMENT_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.i18n.Message;
import io.quarkus.qute.i18n.MessageBundle;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

/**
 * The generated example properties files follow {@code quarkus.qute.localized-file-keys}.
 */
public class LocalizedFileMessageKeyExampleFileTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot(root -> root
                    .addClasses(Messages.class))
            .overrideConfigKey("quarkus.qute.localized-file-keys", "message-key");

    @ProdBuildResults
    ProdModeTestResults testResults;

    @Test
    public void testExampleProperties() throws IOException {
        Path path = testResults.getBuildDir().resolve("qute-i18n-examples").resolve("exa.properties");
        assertTrue(path.toFile().canRead());
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream(path.toFile())) {
            props.load(in);
        }
        assertEquals(2, props.size());
        assertEquals("Hello {name} and more!", props.getProperty("hello_and_more"));
        assertEquals("Custom!", props.getProperty("custom"));
    }

    @MessageBundle(value = "exa", locale = "en", defaultKey = UNDERSCORED_ELEMENT_NAME)
    public interface Messages {

        @Message("Hello {name} and more!")
        String helloAndMore(String name);

        @Message(value = "Custom!", key = "custom")
        String other();

    }

}
