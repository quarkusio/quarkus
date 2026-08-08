package io.quarkus.arc.test.cdi.bcextensions;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.enterprise.inject.build.compatible.spi.BeanInfo;
import jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension;
import jakarta.enterprise.inject.build.compatible.spi.Registration;
import jakarta.enterprise.inject.spi.DefinitionException;
import jakarta.enterprise.util.TypeLiteral;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.test.ArcTestContainer;

public class RegistrationWithRawTypeLiteralTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .buildCompatibleExtensions(new MyExtension())
            .shouldFail()
            .build();

    @Test
    public void trigger() {
        Throwable error = container.getFailure();
        assertNotNull(error);
        assertInstanceOf(DefinitionException.class, error);
        assertTrue(error.getMessage().contains("Raw TypeLiteral in @Registration.types"));
    }

    public static class MyExtension implements BuildCompatibleExtension {
        @Registration(types = MyRawTypeLiteral.class)
        public void wrong(BeanInfo bean) {
        }

        static class MyRawTypeLiteral extends TypeLiteral {
        }
    }
}
