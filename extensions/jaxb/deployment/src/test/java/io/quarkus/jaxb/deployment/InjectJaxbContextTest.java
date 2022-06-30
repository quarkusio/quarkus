package io.quarkus.jaxb.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.StringWriter;

import javax.inject.Inject;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class InjectJaxbContextTest {

    @Inject
    JAXBContext jaxbContext;

    @Inject
    Marshaller marshaller;

    @Inject
    Unmarshaller unmarshaller;

    @Test
    public void shouldInjectJaxbBeans() {
        assertNotNull(jaxbContext);
        assertNotNull(marshaller);
        assertNotNull(unmarshaller);
    }

    @Test
    public void shouldPersonBeInTheJaxbContext() throws JAXBException {
        Person person = new Person();
        person.setFirst("first");
        person.setLast("last");

        StringWriter sw = new StringWriter();
        marshaller.marshal(person, sw);

        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                + "<person>\n"
                + "    <first>first</first>\n"
                + "    <last>last</last>\n"
                + "</person>\n", sw.toString());
    }

}
