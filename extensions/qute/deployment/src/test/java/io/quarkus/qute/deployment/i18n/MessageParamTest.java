package io.quarkus.qute.deployment.i18n;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.i18n.Message;
import io.quarkus.qute.i18n.MessageBundle;
import io.quarkus.qute.i18n.MessageBundles;
import io.quarkus.qute.i18n.MessageParam;
import io.quarkus.test.QuarkusUnitTest;

public class MessageParamTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyBundle.class));

    @Test
    public void testValidation() {
        Assertions.assertEquals("Hello there!", MessageBundles.get(MyBundle.class).hello("there", "!"));
    }

    @MessageBundle
    public interface MyBundle {

        @Message("Hello {name}{suffix}")
        String hello(@MessageParam("name") String foo, String suffix);

    }

}
