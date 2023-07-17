package io.quarkus.hibernate.orm.panache.deployment.test;

import jakarta.persistence.Entity;
import jakarta.persistence.Transient;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElements;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
@XmlRootElement(name = "JAXBEntity")
public class JAXBEntity extends PanacheEntity {

    @XmlAttribute(name = "Named")
    public String namedAnnotatedProp;

    @XmlTransient
    public String transientProp;

    @XmlAttribute
    public String defaultAnnotatedProp;

    @XmlElements({
            @XmlElement(name = "array1"),
            @XmlElement(name = "array2")
    })
    public String arrayAnnotatedProp;

    public String unAnnotatedProp;

    // note that this annotation is automatically added for mapped fields, which is not the case here
    // so we do it manually to emulate a mapped field situation
    @XmlTransient
    @Transient
    public int serialisationTrick;

    public String name;

    // For JAXB: both getter and setter are required
    // Here we make sure the field is not used by Hibernate, but the accessor is used by jaxb, jsonb and jackson
    public int getSerialisationTrick() {
        return ++serialisationTrick;
    }

    public void setSerialisationTrick(int serialisationTrick) {
        this.serialisationTrick = serialisationTrick;
    }
}
