package io.quarkus.qute.deployment.extensions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Template;
import io.quarkus.test.QuarkusUnitTest;

public class ConfigTemplateExtensionsTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset(
                            "{config:foo}={config:property('foo')}\n" +
                                    "{config:nonExistent ?: 'Not found!'}={config:property('nonExistent') ?: 'Not found!'}\n" +
                                    "{config:['foo.bar.baz']}={config:property('foo.bar.baz')}\n" +
                                    "{config:['quarkus.qute.remove-standalone-lines']}={config:property('quarkus.qute.remove-standalone-lines')}\n"
                                    +
                                    "{config:property(name)}\n" +
                                    "{config:boolean('foo.bool') ?: 'Not found!'} {config:boolean('foo.boolean') ?: 'Not found!'}\n"
                                    +
                                    "{config:integer('foo.bar.baz')}"),
                            "templates/foo.html")
                    .addAsResource(new StringAsset("foo=false\nfoo.bar.baz=11\nfoo.bool=true"), "application.properties"));

    @Inject
    Template foo;

    @Test
    public void testGetProperty() {
        assertEquals("false=false\n" +
                "Not found!=Not found!\n" +
                "11=11\n" +
                "true=true\n" +
                "false\n" +
                "true Not found!\n" +
                "11",
                foo.data("name", "foo").render());
    }

}
