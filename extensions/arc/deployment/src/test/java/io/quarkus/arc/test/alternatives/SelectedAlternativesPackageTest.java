package io.quarkus.arc.test.alternatives;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.test.alternatives.bar.Bar;
import io.quarkus.arc.test.alternatives.bar.MyStereotype;
import io.quarkus.test.QuarkusUnitTest;

public class SelectedAlternativesPackageTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(SelectedAlternativesPackageTest.class, Alpha.class, Producers.class, Foo.class, Bar.class,
                            MyStereotype.class)
                    .addAsResource(new StringAsset(
                            "quarkus.arc.selected-alternatives=io.quarkus.arc.test.alternatives.*"),
                            "application.properties"));

    @Inject
    Instance<Alpha> alpha;

    @Inject
    Instance<String> bravo;

    @Inject
    Instance<Integer> charlie;

    @Inject
    Instance<Foo> foo;

    @Test
    public void testSelectedAlternatives() {
        assertTrue(alpha.isResolvable());
        assertEquals("ok", alpha.get().ping());
        assertTrue(bravo.isResolvable());
        assertEquals("bravo", bravo.get());
        assertTrue(charlie.isResolvable());
        assertEquals(10, charlie.get());
        // Bar/MyStereotype is not selected because its package is not "io.quarkus.arc.test.alternatives"
        assertTrue(foo.isResolvable());
        assertEquals(Foo.class.getName(), foo.get().ping());
    }

    @Alternative
    @ApplicationScoped
    static class Alpha {

        public String ping() {
            return "ok";
        }

    }

}
