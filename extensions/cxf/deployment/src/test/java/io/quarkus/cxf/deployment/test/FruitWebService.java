package io.quarkus.cxf.deployment.test;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;

@WebService(targetNamespace = "http://test.deployment.cxf.quarkus.io/", name = "FruitWebServiceImplPortType")
public interface FruitWebService {

    @WebMethod
    int count();

    @WebMethod
    void add(@WebParam(name = "fruit") Fruit fruit);

    @WebMethod
    void delete(@WebParam(name = "fruit") Fruit fruit);
}
