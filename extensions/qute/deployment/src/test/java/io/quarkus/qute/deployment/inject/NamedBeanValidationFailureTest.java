package io.quarkus.qute.deployment.inject;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.TemplateException;
import io.quarkus.test.QuarkusUnitTest;

public class NamedBeanValidationFailureTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(NamedFoo.class)
                    .addAsResource(new StringAsset("{#each inject:foo.list}{it.ping}{/each}}"), "templates/fooping.html"))
            .assertException(t -> {
                Throwable e = t;
                TemplateException te = null;
                while (e != null) {
                    if (e instanceof TemplateException) {
                        te = (TemplateException) e;
                        break;
                    }
                    e = e.getCause();
                }
                assertNotNull(te);
                assertTrue(te.getMessage().contains("Found template problems (1)"), te.getMessage());
                assertTrue(te.getMessage().contains("it.ping"), te.getMessage());
            });

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
