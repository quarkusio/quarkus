package io.quarkus.qute.deployment.inject;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Template;
import io.quarkus.test.QuarkusUnitTest;

public class NamedBeanValidationSuccessTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(NamedFoo.class)
                    .addAsResource(new StringAsset("{#each inject:foo.list}{it.length}{/each}"), "templates/fooping.html"));

    @Inject
    Template fooping;

    @Test
    public void testResult() {
        assertEquals("3", fooping.render());
    }

    @ApplicationScoped
    @Named("foo")
    public static class NamedFoo {

        public List<String> getList() {
            return Collections.singletonList("one");
        }

    }

}
