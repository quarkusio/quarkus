package io.quarkus.cxf.deployment.test;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import io.quarkus.cxf.deployment.test.impl.FruitImpl;

public class FruitAdapter extends XmlAdapter<FruitImpl, Fruit> {
    public FruitImpl marshal(Fruit fruit) throws Exception {
        if (fruit instanceof FruitImpl) {
            return (FruitImpl) fruit;
        }
        return new FruitImpl(fruit.getName(), fruit.getDescription());
    }

    public Fruit unmarshal(FruitImpl fruit) throws Exception {
        return fruit;
    }
}