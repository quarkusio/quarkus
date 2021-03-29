package io.quarkus.hibernate.validator.test.injection;

public class TestBean {

    @TestConstraint
    public String name;

}