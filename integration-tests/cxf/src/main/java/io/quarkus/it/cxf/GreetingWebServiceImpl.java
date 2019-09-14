package io.quarkus.it.cxf;

import javax.jws.WebParam;
import javax.jws.WebService;

@WebService(endpointInterface = "io.quarkus.it.cxf.GreetingWebService", serviceName = "GreetingWebService")
public class GreetingWebServiceImpl implements GreetingWebService {

    @Override
    public String reply(@WebParam(name = "text") String text) {
        return "Hello " + text;
    }
}
