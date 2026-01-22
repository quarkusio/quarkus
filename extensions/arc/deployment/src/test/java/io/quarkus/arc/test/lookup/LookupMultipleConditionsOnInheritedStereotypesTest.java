package io.quarkus.arc.test.lookup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Stereotype;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.lookup.LookupIfProperty;
import io.quarkus.test.QuarkusUnitTest;

public class LookupMultipleConditionsOnInheritedStereotypesTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(AlphaStereotype.class, BravoStereotype.class,
                    AlphaBravoStereotype.class, CharlieStereotype.class, CharlieDeltaStereotype.class,
                    EchoFoxtrotStereotype.class, FirstService.class, SecondService.class, ThirdService.class))
            .overrideConfigKey("service.alpha.enabled", "true")
            .overrideConfigKey("service.delta.enabled", "true")
            .overrideConfigKey("service.echo.enabled", "true")
            .overrideConfigKey("service.foxtrot.enabled", "true");

    @Inject
    Instance<FirstService> first;

    @Inject
    Instance<SecondService> second;

    @Inject
    Instance<ThirdService> third;

    @Test
    public void testConditions() {
        assertTrue(first.isResolvable());
        assertEquals("first", first.get().ping());
        assertTrue(second.isUnsatisfied());
        assertTrue(third.isResolvable());
        assertEquals("third", third.get().ping());
    }

    @Stereotype
    @LookupIfProperty(name = "service.alpha.enabled", stringValue = "true")
    @Retention(RetentionPolicy.RUNTIME)
    @interface AlphaStereotype {
    }

    @Stereotype
    @LookupIfProperty(name = "service.bravo.enabled", stringValue = "true", lookupIfMissing = true)
    @Retention(RetentionPolicy.RUNTIME)
    @interface BravoStereotype {
    }

    @Stereotype
    @AlphaStereotype
    @BravoStereotype
    @Retention(RetentionPolicy.RUNTIME)
    @Inherited
    @interface AlphaBravoStereotype {
    }

    @Stereotype
    @LookupIfProperty(name = "service.charlie.enabled", stringValue = "true")
    @Retention(RetentionPolicy.RUNTIME)
    @interface CharlieStereotype {
    }

    @Stereotype
    @CharlieStereotype
    @LookupIfProperty(name = "service.delta.enabled", stringValue = "true")
    @Retention(RetentionPolicy.RUNTIME)
    @Inherited
    @interface CharlieDeltaStereotype {
    }

    @Stereotype
    @LookupIfProperty(name = "service.echo.enabled", stringValue = "true")
    @LookupIfProperty(name = "service.foxtrot.enabled", stringValue = "true")
    @Retention(RetentionPolicy.RUNTIME)
    @Inherited
    @interface EchoFoxtrotStereotype {
    }

    @AlphaBravoStereotype
    static class FirstServiceParent {
    }

    @Singleton
    static class FirstService extends FirstServiceParent {
        public String ping() {
            return "first";
        }
    }

    @CharlieDeltaStereotype
    static class SecondServiceParent {
    }

    @Singleton
    static class SecondService extends SecondServiceParent {
        public String ping() {
            return "service";
        }
    }

    @EchoFoxtrotStereotype
    static class ThirdServiceParent {
    }

    @Singleton
    static class ThirdService extends ThirdServiceParent {
        public String ping() {
            return "third";
        }
    }
}
