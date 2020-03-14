package io.quarkus.cxf.deployment.test;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Set;

import javax.jws.WebParam;
import javax.jws.WebService;

@WebService(endpointInterface = "io.quarkus.cxf.deployment.test.FruitWebService", serviceName = "FruitWebService")
public class FruitWebServiceImpl implements FruitWebService {

    private Set<Fruit> fruits = Collections.newSetFromMap(Collections.synchronizedMap(new LinkedHashMap<>()));

    public FruitWebServiceImpl() {
        fruits.add(new Fruit("Apple", "Winter fruit"));
        fruits.add(new Fruit("Pineapple", "Tropical fruit"));
    }

    @Override
    public int count() {
        return (fruits != null ? fruits.size() : 0);
    }

    @Override
    public void add(@WebParam(name = "fruit") Fruit fruit) {
        fruits.add(fruit);
    }

    @Override
    public void delete(@WebParam(name = "fruit") Fruit fruit) {
        fruits.remove(fruit);
    }
}
