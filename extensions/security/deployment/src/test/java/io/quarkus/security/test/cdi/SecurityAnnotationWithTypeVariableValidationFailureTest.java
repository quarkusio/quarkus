package io.quarkus.security.test.cdi;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.annotation.security.DenyAll;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.spi.SecuredInterfaceAnnotationBuildItem;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Type variables are currently not supported for secured interfaces.
 */
public class SecurityAnnotationWithTypeVariableValidationFailureTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest().setArchiveProducer(() -> ShrinkWrap
            .create(JavaArchive.class)
            .addClasses(SecuredInterface.class, ParametrizedType.class, Repository.class, SecuredInterfaceImpl.class))
            .assertException(throwable -> {
                assertInstanceOf(RuntimeException.class, throwable);
                String exceptionMessage = throwable.getMessage();
                assertTrue(exceptionMessage.contains("Unable to determine if the"),
                        () -> "Unexpected exception message: " + exceptionMessage);
                assertTrue(exceptionMessage.contains("SecuredInterfaceImpl#securedMethod"),
                        () -> "Unexpected exception message: " + exceptionMessage);
                assertTrue(exceptionMessage.contains("method should inherit security annotation"),
                        () -> "Unexpected exception message: " + exceptionMessage);
                assertTrue(exceptionMessage.contains("SecuredInterface#securedMethod"),
                        () -> "Unexpected exception message: " + exceptionMessage);
            })
            .addBuildChainCustomizer(buildChainBuilder -> buildChainBuilder
                    .addBuildStep(context -> context
                            .produce(SecuredInterfaceAnnotationBuildItem.ofClassAnnotation(Repository.class.getName())))
                    .produces(SecuredInterfaceAnnotationBuildItem.class).build());

    @Test
    public void runTest() {
        Assertions.fail("This test should not run");
    }

    @Repository
    public interface SecuredInterface<T> {

        @DenyAll
        Object securedMethod(ParametrizedType<T> securedAnnotation);

    }

    public static class SecuredInterfaceImpl implements SecuredInterface<String> {

        @Override
        public Object securedMethod(ParametrizedType<String> securedAnnotation) {
            return null;
        }
    }

    public static class ParametrizedType<T> {

    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface Repository {

    }
}
