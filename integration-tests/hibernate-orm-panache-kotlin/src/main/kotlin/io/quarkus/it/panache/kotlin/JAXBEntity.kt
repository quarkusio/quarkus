package io.quarkus.it.panache.kotlin

import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntity
import javax.persistence.Entity
import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlAttribute
import javax.xml.bind.annotation.XmlElement
import javax.xml.bind.annotation.XmlElements
import javax.xml.bind.annotation.XmlRootElement
import javax.xml.bind.annotation.XmlTransient

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