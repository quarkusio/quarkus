package io.quarkus.qute.deployment.extensions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.function.IntFunction;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Template;
import io.quarkus.test.QuarkusUnitTest;

public class ObjectsTemplateExtensionsTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(root -> root
                    .addAsResource(new StringAsset(
                            "{name == 'hallo' ? 'yes' : 'no'}::{name eq 'hello' ? 'yes' : 'no'}::{name is 'hi' ? 'yes' : 'no'}::{#if name eq 'hallo'}ok{/if}"
                                    + "::{fun.apply(name eq 'hello' ? 0 : 7)}"),
                            "templates/foo.html"));

    @Inject
    Template foo;

    @Test
    public void testEquals() {
        assertEquals("yes::no::no::ok::ok", foo
                .data("name", "hallo")
                .data("fun", new IntFunction<String>() {

                    @Override
                    public String apply(int value) {
                        return value > 5 ? "ok" : "nok";
                    }
                })
                .render());
    }

}
