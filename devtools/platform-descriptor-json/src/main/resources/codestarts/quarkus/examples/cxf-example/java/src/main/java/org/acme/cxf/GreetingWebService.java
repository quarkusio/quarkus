package org.acme.cxf;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.ws.RequestWrapper;

@WebService
public interface GreetingWebService {

    @WebMethod
    String reply(@WebParam(name = "text") String text);

    @WebMethod
    @RequestWrapper(localName = "Ping", targetNamespace = "http://cxf.acme.org/", className = "org.acme.cxf.Ping")
    String ping(@WebParam(name = "text") String text);

}
