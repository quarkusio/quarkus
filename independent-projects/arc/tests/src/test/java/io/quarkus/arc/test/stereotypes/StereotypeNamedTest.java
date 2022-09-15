package io.quarkus.arc.test.stereotypes;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Stereotype;
import jakarta.inject.Named;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class StereotypeNamedTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(BeNamed.class, IamNamed.class);

    @Test
    public void testStereotype() {
        assertTrue(Arc.container().instance("iamNamed").isAvailable());
    }

    @Named
    @Stereotype
    @Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
    @Retention(RetentionPolicy.RUNTIME)
    public @interface BeNamed {
    }

    @ApplicationScoped
    @BeNamed
    static class IamNamed {

        public String getId() {
            return "OK";
        }

    }

}
