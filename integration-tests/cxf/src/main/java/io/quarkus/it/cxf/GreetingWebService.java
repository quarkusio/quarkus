package io.quarkus.it.cxf;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;

@WebService
public interface GreetingWebService {

    @WebMethod //(operationName = "reply", action="http://example.org/reply")
    String reply(@WebParam(name = "text") String text);
}
