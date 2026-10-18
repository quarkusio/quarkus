package io.quarkus.qute.deployment.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.i18n.Message;
import io.quarkus.qute.i18n.MessageBundle;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class MessageBundleEnumExampleFileTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot(root -> root
                    .addClasses(Messages.class, MyEnum.class)
                    .addAsResource(new StringAsset("""
                            myEnum_ON=On
                            myEnum_OFF=Off
                            myEnum_UNDEFINED=Undefined
                            """),
                            "messages/enu.properties"));

    @ProdBuildResults
    ProdModeTestResults testResults;

    @Test
    public void testExampleProperties() throws FileNotFoundException, IOException {
        Path path = testResults.getBuildDir().resolve("qute-i18n-examples").resolve("enu.properties");
        assertTrue(path.toFile().canRead());
        Properties props = new Properties();
        props.load(new FileInputStream(path.toFile()));
        assertEquals(3, props.size());
        assertTrue(props.containsKey("myEnum_ON"));
        assertTrue(props.containsKey("myEnum_OFF"));
        assertTrue(props.containsKey("myEnum_UNDEFINED"));
    }

    @MessageBundle(value = "enu", locale = "en")
    public interface Messages {

        // Replaced with:
        // @Message("{#when myEnum}"
        //  + "{#is ON}{enu:myEnum_ON}"
        //  + "{#is OFF}{enu:myEnum_OFF}"
        //  + "{#is UNDEFINED}{enu:myEnum_UNDEFINED}"
        //  + "{/when}")
        @Message
        String myEnum(MyEnum myEnum);

    }

}
