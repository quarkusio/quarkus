package io.quarkus.qute.deployment.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Template;
import io.quarkus.test.QuarkusUnitTest;

public class NotElementNameFileTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(
                    root -> root.addClass(CharlieMessages.class)
                            .addAsResource(new StringAsset("hello_and_more=Wazup!\nhello-and-less=Jo!"),
                                    "messages/msg_en.properties")
                            .addAsResource(new StringAsset("{msg:hello_and_more}"), "templates/more.html")
                            .addAsResource(new StringAsset("{msg:hello-and-less}"), "templates/less.html"))
            .overrideConfigKey("quarkus.default-locale", "en");

    @Inject
    Template more;

    @Inject
    Template less;

    @Inject
    CharlieMessages messages;

    @Test
    public void testTemplate() {
        String wazup = more.render();
        assertEquals("Wazup!", wazup);
        String jo = less.render();
        assertEquals("Jo!", jo);
    }

    @Test
    public void testInterface() {
        assertEquals("Wazup!", messages.helloAndMore());
        assertEquals("Jo!", messages.helloAndLess());
    }

}
