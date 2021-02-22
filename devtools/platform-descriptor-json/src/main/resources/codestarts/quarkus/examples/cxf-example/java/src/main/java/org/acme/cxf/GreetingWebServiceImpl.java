package org.acme.cxf;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.ws.BindingType;

@WebService(endpointInterface = "org.acme.cxf.GreetingWebService", serviceName = "GreetingWebService")
@BindingType(javax.xml.ws.soap.SOAPBinding.SOAP12HTTP_BINDING)
public class GreetingWebServiceImpl implements GreetingWebService {

    @Inject
    public HelloResource helloResource;

    @Override
    public String reply(@WebParam(name = "text") String text) {
        return helloResource.getHello() + text;
    }

    @Override
    public String ping(@WebParam(name = "text") String text) {
        return helloResource.getHello() + text;
    }

}
