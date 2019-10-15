package io.quarkus.cxf.deployment.test.impl;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Set;

import javax.jws.WebService;

import io.quarkus.cxf.deployment.test.Fruit;
import io.quarkus.cxf.deployment.test.FruitWebService;

@WebService(endpointInterface = "org.acme.cxf.FruitWebService")
public class FruitWebServiceImpl implements FruitWebService {

    private Set<Fruit> fruits = Collections.newSetFromMap(Collections.synchronizedMap(new LinkedHashMap<>()));

    public FruitWebServiceImpl() {
        fruits.add(new FruitImpl("Apple", "Winter fruit"));
        fruits.add(new FruitImpl("Pineapple", "Tropical fruit"));
    }

    @Override
    public Set<Fruit> list() {
        return fruits;
    }

    @Override
    public Set<Fruit> add(Fruit fruit) {
        fruits.add(fruit);
        return fruits;
    }

    @Override
    public Set<Fruit> delete(Fruit fruit) {
        fruits.remove(fruit);
        return fruits;
    }
}