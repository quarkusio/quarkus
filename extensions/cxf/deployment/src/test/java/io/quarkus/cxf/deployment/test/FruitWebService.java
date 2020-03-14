package io.quarkus.cxf.deployment.test;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;

@WebService
public interface FruitWebService {

    @WebMethod
    @WebResult(name = "countFruitsResponse", targetNamespace = "http://test.deployment.cxf.quarkus.io/", partName = "parameters")
    int count();

    @WebMethod
    void add(@WebParam(name = "fruit") Fruit fruit);

    @WebMethod
    void delete(@WebParam(name = "fruit") Fruit fruit);
}
