package io.quarkus.qute.deployment.i18n;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.TemplateEnum;
import io.quarkus.qute.deployment.MessageBundleException;
import io.quarkus.qute.i18n.Message;
import io.quarkus.qute.i18n.MessageBundle;
import io.quarkus.test.QuarkusUnitTest;

public class MessageBundleInvalidEnumConstantTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(root -> root
                    .addClasses(Messages.class, UnderscoredEnum.class)
                    .addAsResource("messages/enu_invalid.properties"))
            .setExpectedException(MessageBundleException.class, true);

    @Test
    public void testMessages() {
        fail();
    }

    @TemplateEnum
    public enum UnderscoredEnum {

        A_B,

    }

    @MessageBundle(value = "enu_invalid")
    public interface Messages {

        @Message
        String underscored(UnderscoredEnum constants);

    }

}
