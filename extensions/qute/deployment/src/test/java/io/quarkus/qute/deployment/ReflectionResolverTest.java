package io.quarkus.qute.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Template;
import io.quarkus.test.QuarkusUnitTest;

public class ReflectionResolverTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(HelloReflect.class)
                    // Make sure we do not detect the template data
                    .addAsResource(new StringAsset("quarkus.qute.detect-template-data=false"), "application.properties")
                    .addAsResource(new StringAsset("{age}:{ping}:{noMatch}"), "META-INF/resources/templates/reflect.txt"));

    @Inject
    Template reflect;

    @Test
    public void testInjection() {
        assertEquals("10:pong:NOT_FOUND", reflect.render(new HelloReflect()));
    }

}
