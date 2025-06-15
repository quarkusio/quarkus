package io.quarkus.jaxb.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.StringWriter;

import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

/**
 * Make sure that the default JAXBContext passes the validation thanks to
 * {@code quarkus.jaxb.exclude-classes=io.quarkus.jaxb.deployment.two.Model} even though there are conflicting classes
 * in the application.
 */
public class InjectJaxbContextTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(io.quarkus.jaxb.deployment.one.Model.class, io.quarkus.jaxb.deployment.two.Model.class,
                            Person.class, CustomJaxbContextCustomizer.class)
                    .addPackage("io.quarkus.jaxb.deployment.info"))
            .overrideConfigKey("quarkus.jaxb.exclude-classes", "io.quarkus.jaxb.deployment.two.Model");

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
    public void packageInfoLoaded() {
        /* make sure the package-info.class is present in the test archive */
        assertThat(io.quarkus.jaxb.deployment.info.Foo.class.getPackage()
                .getAnnotation(jakarta.xml.bind.annotation.XmlSchema.class)).isNotNull();
    }

    @Test
    @ActivateRequestContext
    public void shouldPersonBeInTheJaxbContext() throws JAXBException {
        Person person = new Person();
        person.setFirst("first");
        person.setLast("last");

        StringWriter sw = new StringWriter();
        marshaller.marshal(person, sw);

        assertThat(sw.toString()).isEqualTo("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                + "<person>\n" + "    <first>first</first>\n" + "    <last>last</last>\n" + "</person>\n");
    }

}
