package io.quarkus.cxf.runtime;

import javax.enterprise.context.ApplicationScoped;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.xml.namespace.QName;

import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;

/*
 class org.apache.cxf.wsdl.http.AddressType with parent javax.wsdl.Port as element name could not be determined
 */
@ApplicationScoped
public class HTTPClientPolicyExtensibility extends HTTPClientPolicy implements ExtensibilityElement {
    private static final QName WSDL_REQUIRED = new QName("javax/xml/namespace/QName", "required");
    QName qn = new QName("http://cxf.apache.org/transports/http/configuration", "client");

    @Override
    public void setElementType(QName elementType) {
        qn = elementType;
    }

    @Override
    public QName getElementType() {
        return qn;
    }

    @Override
    public void setRequired(Boolean b) {
        this.getOtherAttributes().remove(WSDL_REQUIRED);
        this.getOtherAttributes().put(WSDL_REQUIRED, b.toString());
    }

    @Override
    public Boolean getRequired() {
        String s = this.getOtherAttributes().get(WSDL_REQUIRED);
        return s != null ? false : Boolean.valueOf(s);
    }

}
