package io.quarkus.qute.deployment;

import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class NamedBeanPropertyNotFoundTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(NamedFoo.class)
                    .addAsResource(new StringAsset("{inject:foo.list.ping}"), "META-INF/resources/templates/fooping.html"))
            .setExpectedException(TemplateException.class);

    @Test
    public void testValidation() {
        fail();
    }

    @ApplicationScoped
    @Named("foo")
    public static class NamedFoo {

        public List<String> getList() {
            return null;
        }

    }

}
