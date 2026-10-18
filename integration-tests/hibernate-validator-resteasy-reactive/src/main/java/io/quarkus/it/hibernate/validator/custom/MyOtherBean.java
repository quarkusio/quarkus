package io.quarkus.it.hibernate.validator.custom;

@MyCustomConstraint
public class MyOtherBean {

    private String name;

    public MyOtherBean(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
