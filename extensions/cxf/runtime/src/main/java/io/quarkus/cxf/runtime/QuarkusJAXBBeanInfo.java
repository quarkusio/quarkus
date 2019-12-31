package io.quarkus.cxf.runtime;

import java.util.Collection;
import java.util.Collections;

import javax.xml.namespace.QName;

import org.apache.cxf.common.jaxb.JAXBBeanInfo;
import org.apache.ws.commons.schema.XmlSchemaElement;

public class QuarkusJAXBBeanInfo implements JAXBBeanInfo {
    private final QName typeName;
    private final XmlSchemaElement el;
    private final QName qname;

    public QuarkusJAXBBeanInfo(QName typeName, XmlSchemaElement el, QName qname) {
        this.typeName = typeName;
        this.el = el;
        this.qname = qname;
    }

    public boolean isElement() {
        return el == null ? false : true;
    }

    public Collection<QName> getTypeNames() {
        return Collections.singletonList(typeName);
    }

    public String getElementNamespaceURI(Object object) {
        return qname.getNamespaceURI();
    }

    public String getElementLocalName(Object object) {
        return qname.getLocalPart();
    }
}
