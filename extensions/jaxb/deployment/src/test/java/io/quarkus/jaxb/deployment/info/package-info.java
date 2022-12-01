/**
 * The inclusion of this file is to verify that the produced JAXBContext works fine with it, otherwise the build fails.
 */
@XmlSchema(namespace = "http://abc.com", xmlns = {
        @XmlNs(prefix = "abc", namespaceURI = "http://abc.com")
})
package io.quarkus.jaxb.deployment.info;

import javax.xml.bind.annotation.XmlNs;
import javax.xml.bind.annotation.XmlSchema;
