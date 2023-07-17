package io.quarkus.qute.deployment.inject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Unremovable;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateException;
import io.quarkus.test.QuarkusUnitTest;

public class InjectionFailedTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(root -> root
                    .addClass(Client.class)
                    .addAsResource(new StringAsset("{this}"), "templates/bar.txt")
                    .addAsResource(new StringAsset("<strong>{this}</strong>"), "templates/bar.qute.html")
                    .addAsResource(new StringAsset("{this}"), "templates/bars/baz.html"))
            .assertException(t -> {
                Throwable e = t;
                Throwable[] suppressed = e.getSuppressed();
                assertEquals(2, suppressed.length);
                for (Throwable s : suppressed) {
                    assertTrue(s instanceof TemplateException);
                    assertTrue(s.getMessage().contains("bar.qute.html"), s.getMessage());
                    assertTrue(s.getMessage().contains("bar.txt"), s.getMessage());
                    assertTrue(s.getMessage().contains("bars/baz.html"), s.getMessage());
                    assertTrue(s.getMessage().contains(
                            "No template found for path [foo] defined at io.quarkus.qute.deployment.inject.InjectionFailedTest#foo")
                            || s.getMessage().contains(
                                    "No template found for path [alpha] defined at io.quarkus.qute.deployment.inject.InjectionFailedTest$Client()"),
                            s.getMessage());
                }
            });

    @Inject
    Template foo;

    @Test
    public void test() {
        fail();
    }

    @Unremovable
    @Singleton
    static class Client {

        Client(Template alpha) {
        }

    }

}
