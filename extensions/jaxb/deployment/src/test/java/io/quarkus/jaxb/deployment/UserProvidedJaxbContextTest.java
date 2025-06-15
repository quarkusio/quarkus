package io.quarkus.jaxb.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.StringWriter;

import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

/**
 * If the user provides his own {@link JAXBContext} bean then the validation of the default context should not happen
 * and the presence of conflicting classes, such as {@link io.quarkus.jaxb.deployment.one.Model} and
 * {@link io.quarkus.jaxb.deployment.two.Model} should not matter.
 */
public class UserProvidedJaxbContextTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(io.quarkus.jaxb.deployment.one.Model.class,
                    io.quarkus.jaxb.deployment.two.Model.class, UserProvidedJaxbContextTest.JaxbContextProducer.class));

    @Inject
    JAXBContext jaxbContext;

    @Inject
    Marshaller marshaller;

    @Inject
    Unmarshaller unmarshaller;

    @Test
    @ActivateRequestContext
    public void shouldInjectJaxbBeans() {
        assertThat(jaxbContext).isNotNull();
        assertThat(marshaller).isNotNull();
        assertThat(unmarshaller).isNotNull();
    }

    @Test
    @ActivateRequestContext
    public void marshalModelOne() throws JAXBException {
        io.quarkus.jaxb.deployment.one.Model model = new io.quarkus.jaxb.deployment.one.Model();
        model.setName1("name1");

        StringWriter sw = new StringWriter();
        marshaller.marshal(model, sw);

        assertThat(sw.toString()).isEqualTo("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<model><name1>name1</name1></model>");
    }

    @Test
    @ActivateRequestContext
    public void marshalModelTwo() throws JAXBException {
        io.quarkus.jaxb.deployment.two.Model model = new io.quarkus.jaxb.deployment.two.Model();
        model.setName2("name2");
        Assertions.assertThatExceptionOfType(JAXBException.class)
                .isThrownBy(() -> marshaller.marshal(model, new StringWriter())).withMessage(
                        "class io.quarkus.jaxb.deployment.two.Model nor any of its super class is known to this context.");
    }

    public static class JaxbContextProducer {

        @Produces
        @Singleton
        JAXBContext produceJaxbContext() {
            try {
                return JAXBContext.newInstance(new Class[] { io.quarkus.jaxb.deployment.one.Model.class });
            } catch (JAXBException e) {
                throw new RuntimeException("Could not create new JAXBContext", e);
            }
        }

    }

}
