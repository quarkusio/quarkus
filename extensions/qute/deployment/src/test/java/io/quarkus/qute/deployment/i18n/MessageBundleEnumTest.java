package io.quarkus.qute.deployment.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateEnum;
import io.quarkus.qute.i18n.Message;
import io.quarkus.qute.i18n.MessageBundle;
import io.quarkus.test.QuarkusUnitTest;

public class MessageBundleEnumTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Messages.class, MyEnum.class, UnderscoredEnum.class, AnotherUnderscoredEnum.class)
                    .addAsResource("messages/enu.properties")
                    .addAsResource("messages/enu_cs.properties")
                    .addAsResource(new StringAsset(
                            "{enu:myEnum(MyEnum:ON)}::{enu:myEnum(MyEnum:OFF)}::{enu:myEnum(MyEnum:UNDEFINED)}::"
                                    + "{enu:shortEnum(MyEnum:ON)}::{enu:shortEnum(MyEnum:OFF)}::{enu:shortEnum(MyEnum:UNDEFINED)}::"
                                    + "{enu:foo(MyEnum:ON)}::{enu:foo(MyEnum:OFF)}::{enu:foo(MyEnum:UNDEFINED)}::"
                                    + "{enu:locFileOverride(MyEnum:ON)}::{enu:locFileOverride(MyEnum:OFF)}::{enu:locFileOverride(MyEnum:UNDEFINED)}"),
                            "templates/foo.html")
                    .addAsResource(new StringAsset(
                            "{enu:underscored(UnderscoredEnum:A_B)}::{enu:underscored(UnderscoredEnum:FOO_BAR_BAZ)}::{enu:underscored_foo(AnotherUnderscoredEnum:NEXT_B)}::{enu:underscored$foo(AnotherUnderscoredEnum:NEXT_B)}::{enu:uncommon(UncommonEnum:NEXT$B)}"),
                            "templates/bar.html"));

    @Inject
    Template foo;

    @Inject
    Template bar;

    @Test
    public void testMessages() {
        assertEquals("On::Off::Undefined::1::0::U::+::-::_::on::off::undefined", foo.render());
        assertEquals("Zapnuto::Vypnuto::Nedefinováno::1::0::N::+::-::_::zap::vyp::nedef",
                foo.instance().setLocale("cs").render());
        assertEquals("A/B::Foo/Bar/Baz::NEXT::NEXT::NEXT", bar.render());
    }

    @TemplateEnum
    public enum UnderscoredEnum {
        A_B,
        FOO_BAR_BAZ
    }

    @TemplateEnum
    public enum AnotherUnderscoredEnum {
        NEXT_B
    }

    @TemplateEnum
    public enum UncommonEnum {
        NEXT$B
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

        // Replaced with:
        // @Message("{#when myEnum}"
        //  + "{#is ON}{enu:shortEnum_ON}"
        //  + "{#is OFF}{enu:shortEnum_OFF}"
        //  + "{#is UNDEFINED}{enu:shortEnum_UNDEFINED}"
        //  + "{/when}")
        @Message
        String shortEnum(MyEnum myEnum);

        @Message("{#when myEnum}"
                + "{#is ON}+"
                + "{#is OFF}-"
                + "{#else}_"
                + "{/when}")
        String foo(MyEnum myEnum);

        @Message
        String locFileOverride(MyEnum myEnum);

        // maps to underscored_$A_B, underscored_$FOO_BAR_BAZ
        @Message
        String underscored(UnderscoredEnum val);

        // maps to underscored_foo_$NEXT_B
        @Message
        String underscored_foo(AnotherUnderscoredEnum val);

        // maps to underscored$foo_$NEXT_B
        @Message
        String underscored$foo(AnotherUnderscoredEnum val);

        // maps to uncommon_$NEXT$B
        @Message
        String uncommon(UncommonEnum val);

    }

}
