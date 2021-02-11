package io.quarkus.qute.deployment.include;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Template;
import io.quarkus.test.QuarkusUnitTest;

public class IncludeTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource(new StringAsset("{#insert item}NOK{/}:{#insert foo}default foo{/}"), "templates/base.html")
                    .addAsResource(new StringAsset("{#include base}{#item}OK{/}{#foo}my foo{/include}"),
                            "templates/detail.html"));

    @Inject
    Template detail;

    @Test
    public void testIncludeSection() {
        assertEquals("OK:my foo", detail.render());
    }

}
