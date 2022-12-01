package io.quarkus.qute.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.test.QuarkusUnitTest;

public class OrEmptyTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(Templates.class)
                    .addAsResource(new StringAsset(
                            "START{#for pet in pets.orEmpty}...{/for}END::{pets.orEmpty.size}"),
                            "templates/OrEmptyTest/pets.html"));

    @CheckedTemplate
    static class Templates {

        static native TemplateInstance pets(List<Object> pets);

    }

    @Test
    public void testOrEmpty() {
        assertEquals("STARTEND::0", Templates.pets(null).render());
    }

}
