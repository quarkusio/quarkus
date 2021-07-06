package io.quarkus.hibernate.orm.panache.deployment.test;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlTransient;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

public class PanacheJAXBTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(JAXBEntity.class, JAXBTestResource.class));

    @Test
    public void testJaxbAnnotationTransfer() throws Exception {
        // Test for fix to this bug: https://github.com/quarkusio/quarkus/issues/6021

        // Ensure that any JAX-B annotations are properly moved to generated getters
        Method m = JAXBEntity.class.getMethod("getNamedAnnotatedProp");
        XmlAttribute anno = m.getAnnotation(XmlAttribute.class);
        assertNotNull(anno);
        assertEquals("Named", anno.name());
        assertNull(m.getAnnotation(XmlTransient.class));

        m = JAXBEntity.class.getMethod("getDefaultAnnotatedProp");
        anno = m.getAnnotation(XmlAttribute.class);
        assertNotNull(anno);
        assertEquals("##default", anno.name());
        assertNull(m.getAnnotation(XmlTransient.class));

        m = JAXBEntity.class.getMethod("getUnAnnotatedProp");
        assertNull(m.getAnnotation(XmlAttribute.class));
        assertNull(m.getAnnotation(XmlTransient.class));

        m = JAXBEntity.class.getMethod("getTransientProp");
        assertNull(m.getAnnotation(XmlAttribute.class));
        assertNotNull(m.getAnnotation(XmlTransient.class));

        m = JAXBEntity.class.getMethod("getArrayAnnotatedProp");
        assertNull(m.getAnnotation(XmlTransient.class));
        XmlElements elementsAnno = m.getAnnotation(XmlElements.class);
        assertNotNull(elementsAnno);
        assertNotNull(elementsAnno.value());
        assertEquals(2, elementsAnno.value().length);
        assertEquals("array1", elementsAnno.value()[0].name());
        assertEquals("array2", elementsAnno.value()[1].name());

        // Ensure that all original fields were labeled @XmlTransient and had their original JAX-B annotations removed
        ensureFieldSanitized("namedAnnotatedProp");
        ensureFieldSanitized("transientProp");
        ensureFieldSanitized("defaultAnnotatedProp");
        ensureFieldSanitized("unAnnotatedProp");
        ensureFieldSanitized("arrayAnnotatedProp");
    }

    private void ensureFieldSanitized(String fieldName) throws Exception {
        Field f = JAXBEntity.class.getDeclaredField(fieldName);
        assertNull(f.getAnnotation(XmlAttribute.class));
        assertNotNull(f.getAnnotation(XmlTransient.class));
    }

    @Test
    public void testPanacheSerialisation() {
        RestAssured.given().accept(ContentType.XML)
                .when().get("/test/ignored-properties")
                .then().body(is(
                        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><JAXBEntity><id>666</id><name>Eddie</name><serialisationTrick>1</serialisationTrick></JAXBEntity>"));
    }

    @Test
    public void jaxbDeserializationHasAllFields() throws JAXBException {
        // set Up
        JAXBEntity person = new JAXBEntity();
        person.name = "max";
        // do
        JAXBContext jaxbContext = JAXBContext.newInstance(JAXBEntity.class);

        Marshaller marshaller = jaxbContext.createMarshaller();
        StringWriter sw = new StringWriter();
        marshaller.marshal(person, sw);
        assertEquals(
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><JAXBEntity><name>max</name><serialisationTrick>1</serialisationTrick></JAXBEntity>",
                sw.toString());
    }
}
