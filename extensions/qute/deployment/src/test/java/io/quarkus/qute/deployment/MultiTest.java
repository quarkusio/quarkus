package io.quarkus.qute.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.math.RoundingMode;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateData;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Multi;

public class MultiTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(Foo.class)
                    .addAsResource(new StringAsset("{foo.val} is not {foo.val.setScale(2,roundingMode)}"),
                            "templates/foo.txt"));

    @Inject
    Template foo;

    @Test
    public void testCreateMulti() {
        Multi<String> multi = foo.data("roundingMode", RoundingMode.HALF_UP)
                .data("foo", new Foo(new BigDecimal("123.4563"))).createMulti();
        assertEquals("123.4563 is not 123.46", multi
                .collect().in(StringBuffer::new, StringBuffer::append)
                .onItem().transform(StringBuffer::toString)
                .await().indefinitely());
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
