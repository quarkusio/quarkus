package io.quarkus.cxf.deployment.test.impl;

import java.util.Objects;

import javax.xml.bind.annotation.XmlType;

import io.quarkus.cxf.deployment.test.Fruit;

@XmlType(name = "Fruit")
public class FruitImpl implements Fruit {

    private String name;

    private String description;

    public FruitImpl() {
    }

    public FruitImpl(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Fruit)) {
            return false;
        }

        Fruit other = (Fruit) obj;

        return Objects.equals(other.getName(), this.getName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getName());
    }
}
