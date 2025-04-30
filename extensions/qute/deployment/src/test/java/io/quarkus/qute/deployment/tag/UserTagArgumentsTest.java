package io.quarkus.qute.deployment.tag;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Template;
import io.quarkus.test.QuarkusUnitTest;

public class UserTagArgumentsTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset(
                            "{_args.size}::{_args.empty}::{_args.get('name')}::{_args.asHtmlAttributes}::{_args.skip('foo','baz').size}::{#each _args.filter('name')}{it.value}{/each}"),
                            "templates/tags/hello.txt")
                    .addAsResource(new StringAsset("{#hello name=val /}"), "templates/foo.txt"));

    @Inject
    Template foo;

    @Test
    public void testInjection() {
        assertEquals("1::false::Lu::name=\"Lu\"::1::Lu", foo.data("val", "Lu").render());
    }

}
