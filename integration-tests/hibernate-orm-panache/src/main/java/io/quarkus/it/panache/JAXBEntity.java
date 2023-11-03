package io.quarkus.it.panache;

import jakarta.persistence.Entity;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElements;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
@XmlRootElement(name = "JAXBEntity")
@XmlAccessorType(XmlAccessType.NONE)
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
}
