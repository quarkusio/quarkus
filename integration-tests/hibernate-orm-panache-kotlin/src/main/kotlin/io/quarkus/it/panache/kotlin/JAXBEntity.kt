package io.quarkus.it.panache.kotlin

import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntity
import jakarta.persistence.Entity
import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlAttribute
import jakarta.xml.bind.annotation.XmlElement
import jakarta.xml.bind.annotation.XmlElements
import jakarta.xml.bind.annotation.XmlRootElement
import jakarta.xml.bind.annotation.XmlTransient

@Entity
@XmlRootElement(name = "JAXBEntity")
@XmlAccessorType(XmlAccessType.NONE)
open class JAXBEntity : PanacheEntity() {
    @XmlAttribute(name = "Named")
    var namedAnnotatedProp: String? = null

    @XmlTransient
    var transientProp: String? = null

    @XmlAttribute
    var defaultAnnotatedProp: String? = null

    @XmlElements(XmlElement(name = "array1"), XmlElement(name = "array2"))
    var arrayAnnotatedProp: String? = null
    var unAnnotatedProp: String? = null
}
