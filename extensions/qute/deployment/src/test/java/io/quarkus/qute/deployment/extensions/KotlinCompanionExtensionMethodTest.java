package io.quarkus.qute.deployment.extensions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Engine;
import io.quarkus.qute.TemplateExtension;
import io.quarkus.qute.deployment.Foo;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Verifies that non-static methods on inner classes (e.g. Kotlin companion objects)
 * annotated with {@link TemplateExtension} are silently skipped rather than causing
 * a build failure. Kotlin companion objects generate both a static method on the outer
 * class and a non-static delegate on the inner {@code $Companion} class — both
 * annotated with {@code @TemplateExtension}.
 *
 * @see <a href="https://github.com/quarkusio/quarkus/issues/52162">#52162</a>
 */
public class KotlinCompanionExtensionMethodTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Foo.class, KotlinLikeExtensions.class, KotlinLikeExtensions.Companion.class)
                    .addAsResource(new StringAsset("{foo.uppercaseName}"),
                            "templates/companion.txt"));

    @Inject
    Engine engine;

    @Test
    public void testStaticMethodProcessedAndCompanionSkipped() {
        assertEquals("FANTOMAS",
                engine.getTemplate("companion").data("foo", new Foo("Fantomas", 10L)).render());
    }

    /**
     * Simulates what the Kotlin compiler generates for a companion object with {@code @JvmStatic}.
     * The outer class gets a static method and the inner Companion class gets a non-static delegate,
     * both annotated with {@code @TemplateExtension}.
     */
    public static class KotlinLikeExtensions {

        // Static method on the outer class — this is the one Qute should process
        @TemplateExtension
        static String uppercaseName(Foo foo) {
            return foo.name.toUpperCase();
        }

        // Simulates Kotlin's $Companion inner class
        public static class Companion {

            // Non-static delegate — Qute should skip this
            @TemplateExtension
            public String uppercaseName(Foo foo) {
                return foo.name.toUpperCase();
            }
        }
    }
}
