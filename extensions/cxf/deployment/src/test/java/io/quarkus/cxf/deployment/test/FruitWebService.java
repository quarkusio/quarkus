package io.quarkus.cxf.deployment.test;

import java.util.Set;

import javax.jws.WebMethod;
import javax.jws.WebService;

@WebService
public interface FruitWebService {

    @WebMethod
    Set<Fruit> list();

    @WebMethod
    Set<Fruit> add(Fruit fruit);

    @WebMethod
    Set<Fruit> delete(Fruit fruit);
}