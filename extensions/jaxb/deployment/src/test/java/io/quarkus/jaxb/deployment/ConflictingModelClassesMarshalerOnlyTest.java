package io.quarkus.jaxb.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.enterprise.inject.spi.DeploymentException;
import jakarta.inject.Inject;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;

import org.assertj.core.api.Assertions;
import org.glassfish.jaxb.core.v2.runtime.IllegalAnnotationException;
import org.glassfish.jaxb.runtime.v2.runtime.IllegalAnnotationsException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

/**
 * Make sure that the validation of the default JAXB context fails if there conflicting model classes and there is only
 * a {@link Marshaller} injection point (which actually requires a {@link JAXBContext} bean to be available too).
 */
public class ConflictingModelClassesMarshalerOnlyTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("application-enable-validation.properties").withApplicationRoot((jar) -> jar
                    .addClasses(io.quarkus.jaxb.deployment.one.Model.class, io.quarkus.jaxb.deployment.two.Model.class))
            .assertException(e -> {
                assertThat(e).isInstanceOf(DeploymentException.class);
                assertThat(e.getMessage()).isEqualTo("Failed to create or validate the default JAXBContext");
                Throwable cause = e.getCause();
                assertThat(cause).isInstanceOf(IllegalAnnotationsException.class);
                assertThat(cause.getMessage()).isEqualTo("1 counts of IllegalAnnotationExceptions");
                List<IllegalAnnotationException> errors = ((IllegalAnnotationsException) cause).getErrors();
                assertThat(errors.size()).isEqualTo(1);
                assertThat(errors.get(0).getMessage()).contains("Two classes have the same XML type name \"model\"");

            });

    @Inject
    Marshaller marshaller;

    @Test
    @ActivateRequestContext
    public void shouldFail() {
        Assertions.fail("The application should fail at boot");
    }

}
