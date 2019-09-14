package io.quarkus.cxf.deployment.test;

import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;

@WebService
public interface HelloWebService {

    @WebResult(targetNamespace = "http://test.deployment.cxf.quarkus.io/")
    String sayHi(@WebParam(targetNamespace = "http://test.deployment.cxf.quarkus.io/") String name);

}
