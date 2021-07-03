package io.quarkus.qute.deployment.extensions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Template;
import io.quarkus.test.QuarkusUnitTest;

public class ConfigTemplateExtensionsTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource(new StringAsset(
                            "{config:foo}={config:property('foo')} {config:nonExistent ?: 'NOT_FOUND'}={config:property('nonExistent') ?: 'NOT_FOUND'} {config:['foo.bar.baz']}={config:property('foo.bar.baz')} {config:['quarkus.qute.remove-standalone-lines']}={config:property('quarkus.qute.remove-standalone-lines')} {config:property(name)}"),
                            "templates/foo.html")
                    .addAsResource(new StringAsset("foo=false\nfoo.bar.baz=11"), "application.properties"));

    @Inject
    Template foo;

    @Test
    public void testGetProperty() {
        assertEquals("false=false NOT_FOUND=NOT_FOUND 11=11 true=true false",
                foo.data("name", "foo").render());
    }

}
