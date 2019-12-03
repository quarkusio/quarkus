package io.quarkus.qute.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.math.RoundingMode;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateData;
import io.quarkus.test.QuarkusUnitTest;

public class TemplateDataTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(Foo.class)
                    .addAsResource(new StringAsset("{foo.val} is not {foo.val.setScale(2,roundingMode)}"),
                            "templates/foo.txt"));

    @Inject
    Template foo;

    @Test
    public void testTemplateData() {
        assertEquals("123.4563 is not 123.46",
                foo.data("roundingMode", RoundingMode.HALF_UP).data("foo", new Foo(new BigDecimal("123.4563"))).render());
    }

    @TemplateData
    @TemplateData(target = BigDecimal.class)
    public static class Foo {

        public final BigDecimal val;

        public Foo(BigDecimal val) {
            this.val = val;
        }

    }

}
