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
                    .addClasses(Foo.class, Foos.class, TransactionType.class)
                    .addAsResource(new StringAsset(
                            "{foo.val} is not {foo.val.setScale(2,roundingMode)} and {foo.bar}={foo.hasBar} and {foo.baz}={foo.isBaz}"),
                            "templates/foo.txt")
                    .addAsResource(new StringAsset(
                            "{#if tx == TransactionType:FOO}OK{/if}::{io_quarkus_qute_deployment_TemplateDataTest_Foos:BRAVO.toLowerCase}"),
                            "templates/bar.txt"));

    @Inject
    Template foo;

    @Inject
    Template bar;

    @Test
    public void testTemplateData() {
        assertEquals("123.4563 is not 123.46 and true=true and false=false",
                foo.data("roundingMode", RoundingMode.HALF_UP).data("foo", new Foo(new BigDecimal("123.4563"))).render());
        assertEquals("OK::bravo", bar.data("tx", TransactionType.FOO).render());
    }

    @TemplateData
    @TemplateData(target = BigDecimal.class)
    public static class Foo {

        public final BigDecimal val;

        public Foo(BigDecimal val) {
            this.val = val;
        }

        public boolean hasBar() {
            return true;
        }

        public boolean isBaz() {
            return false;
        }

    }

    // namespace is TransactionType
    @TemplateData(namespace = TemplateData.SIMPLENAME)
    public static enum TransactionType {

        FOO,
        BAR

    }

    // namespace is io_quarkus_qute_deployment_TemplateDataTest_Foos
    @TemplateData
    public static enum Foos {

        ALPHA,
        BRAVO;

        Foos() {
        }

        public String toLowerCase() {
            return this.toString().toLowerCase();
        }

    }

}
