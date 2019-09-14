package io.quarkus.cxf.deployment.test;

import javax.jws.WebService;

@WebService(endpointInterface = "io.quarkus.cxf.deployment.test.HelloWebService")
public class HelloWebServiceImpl implements HelloWebService {

    @Override
    public String sayHi(String name) {
        return "Hello " + name;
    }
}
