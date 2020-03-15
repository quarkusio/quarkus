package io.quarkus.cxf.runtime;

import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;

public class AbstractCxfClientProducer {

    public Object loadCxfClient(String sei, String endpointAddress, String wsdlUrl) {
        Class<?> seiClass;
        try {
            seiClass = Class.forName(sei);
        } catch (ClassNotFoundException e) {
            return null;
        }
        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setServiceClass(seiClass);
        factory.setAddress(endpointAddress);
        factory.setWsdlURL(wsdlUrl);
        return factory.create();
    }
}
